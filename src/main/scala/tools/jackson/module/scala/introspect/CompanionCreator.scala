package tools.jackson.module.scala.introspect

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.deser.CreatorProperty
import tools.jackson.databind.deser.std.StdValueInstantiator
import tools.jackson.databind.introspect.{AnnotatedMethod, AnnotatedParameter, AnnotationMap, TypeResolutionContext}
import tools.jackson.databind.`type`.TypeBindings
import tools.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext, PropertyMetadata, PropertyName}
import tools.jackson.module.scala.util.ClassW

import java.lang.reflect.{InvocationTargetException, Method, Modifier}
import scala.util.Try

/**
 * A `@JsonCreator` on a companion object method that Jackson cannot see.
 *
 * Jackson collects creators from a class's constructors and static methods. A companion's method is
 * neither, but scalac emits a static forwarder for it on the class, and that carries the annotation.
 * Scala 2 emits no forwarders for the companion of a class that is not top level, though - one
 * declared inside an object, say - so there the annotation goes unseen and the constructor is used
 * instead. This finds such a method, so that the value instantiator below can stand in for the
 * forwarder and call it.
 */
private[introspect] object CompanionCreator {

  /**
   * The annotated companion method of `clazz` that the class carries no static forwarder for, if
   * there is one. A forwarder is Jackson's to find, so a method that has one is not reported.
   */
  def hidden(clazz: Class[_]): Option[Method] = {
    val hidden = ClassW.companionClassOf(clazz).toSeq.flatMap { companion =>
      companion.getMethods.filter { method =>
        !Modifier.isStatic(method.getModifiers) &&
          clazz.isAssignableFrom(method.getReturnType) &&
          Option(method.getAnnotation(classOf[JsonCreator])).exists(_.mode() != JsonCreator.Mode.DISABLED) &&
          !hasForwarder(clazz, method)
      }
    }
    if (hidden.length > 1) {
      throw new IllegalArgumentException(s"Conflicting creators on the companion of ${clazz.getName}: " +
        hidden.map(_.getName).mkString(", ") + " - only one @JsonCreator can be used for a class whose companion methods " +
        "have no static forwarders (a class not declared at the top level, compiled by Scala 2)")
    }
    hidden.headOption
  }

  private def hasForwarder(clazz: Class[_], method: Method): Boolean =
    Try(clazz.getMethod(method.getName, method.getParameterTypes: _*)).toOption.exists(f => Modifier.isStatic(f.getModifiers))

  /** The name the parameter is read under: the one `@JsonProperty` gives it, else its own. */
  private[introspect] def parameterName(method: Method, index: Int): String = {
    val parameter = method.getParameters()(index)
    Option(parameter.getAnnotation(classOf[JsonProperty])).map(_.value).filter(_.nonEmpty).getOrElse(parameter.getName)
  }
}

/**
 * Creates values through a companion method Jackson could not see (see [[CompanionCreator]]).
 * Takes over the with-arguments and delegate creation of the instantiator Jackson built, and calls
 * the companion's method where that would have called a static one.
 */
private[introspect] class CompanionCreatorInstantiator(delegate: StdValueInstantiator, config: DeserializationConfig,
                                                        beanDesc: BeanDescription.Supplier, method: Method)
  extends StdValueInstantiator(delegate) {

  import CompanionCreator.parameterName

  private val companion: AnyRef = ClassW.companionOf(beanDesc.getBeanClass)
    .getOrElse(throw new IllegalArgumentException(s"${beanDesc.getBeanClass.getName} has no companion object"))

  {
    val typeFactory = config.getTypeFactory
    val context = new TypeResolutionContext.Basic(typeFactory, TypeBindings.emptyBindings())
    val parameterAnnotations = method.getParameterAnnotations.map(annotations => AnnotationMap.of(java.util.Arrays.asList(annotations: _*)))
    val creator = new AnnotatedMethod(context, method, AnnotationMap.of(java.util.Arrays.asList(method.getAnnotations: _*)), parameterAnnotations)
    val parameterTypes = method.getGenericParameterTypes.map(typeFactory.constructType)
    val names = parameterTypes.indices.map(parameterName(method, _))

    // Jackson's own rule for a creator with one parameter and no mode: property-based when the
    // parameter has a property's name, delegating otherwise
    val mode = method.getAnnotation(classOf[JsonCreator]).mode() match {
      case JsonCreator.Mode.DEFAULT if parameterTypes.length == 1 =>
        val explicit = method.getParameters()(0).isAnnotationPresent(classOf[JsonProperty])
        val named = beanDesc.get().findProperties().stream().anyMatch(_.getName == names(0))
        if (explicit || named) JsonCreator.Mode.PROPERTIES else JsonCreator.Mode.DELEGATING
      case JsonCreator.Mode.DEFAULT => JsonCreator.Mode.PROPERTIES
      case explicit => explicit
    }

    if (mode == JsonCreator.Mode.DELEGATING) {
      configureFromObjectSettings(delegate.getDefaultCreator, creator, parameterTypes(0), None.orNull, None.orNull, None.orNull)
    } else {
      val arguments = parameterTypes.indices.map { index =>
        val parameter = new AnnotatedParameter(creator, parameterTypes(index), context, parameterAnnotations(index), index)
        CreatorProperty.construct(new PropertyName(names(index)), parameterTypes(index), None.orNull, None.orNull,
          beanDesc.get().getClassAnnotations, parameter, index, None.orNull, PropertyMetadata.STD_REQUIRED_OR_OPTIONAL)
          .asInstanceOf[tools.jackson.databind.deser.SettableBeanProperty]
      }.toArray
      configureFromObjectSettings(delegate.getDefaultCreator, None.orNull, None.orNull, None.orNull, creator, arguments)
    }
  }

  override def createFromObjectWith(ctxt: DeserializationContext, args: Array[AnyRef]): AnyRef = invoke(ctxt, args)

  override def createUsingDelegate(ctxt: DeserializationContext, delegate: AnyRef): AnyRef = invoke(ctxt, Array(delegate))

  private def invoke(ctxt: DeserializationContext, args: Array[AnyRef]): AnyRef = {
    try method.invoke(companion, args: _*)
    catch {
      case e: InvocationTargetException => throw ctxt.instantiationException(getValueClass, e.getCause)
      case e: ReflectiveOperationException => throw ctxt.instantiationException(getValueClass, e)
    }
  }
}
