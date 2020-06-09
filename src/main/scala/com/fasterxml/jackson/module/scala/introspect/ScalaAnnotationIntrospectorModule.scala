package com.fasterxml.jackson.module.scala.introspect

import java.lang.annotation.Annotation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext}
import com.fasterxml.jackson.databind.`type`.ClassKey
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.{CreatorProperty, NullValueProvider, SettableBeanProperty, ValueInstantiator, ValueInstantiators}
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.{AccessPattern, SimpleLookupCache}
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicits._

object ScalaAnnotationIntrospector extends NopAnnotationIntrospector with ValueInstantiators {
  private [this] val _descriptorCache = new SimpleLookupCache[ClassKey, BeanDescriptor](16, 100)

  private def _descriptorFor(clz: Class[_]): BeanDescriptor = {
    val key = new ClassKey(clz)
    var result = _descriptorCache.get(key)
    if (result == null) {
      result = BeanIntrospector(clz)
      _descriptorCache.put(key, result)
    }

    result
  }

  private def fieldName(af: AnnotatedField): Option[String] = {
    val d = _descriptorFor(af.getDeclaringClass)
    d.properties.find(p => p.field.exists(_ == af.getAnnotated)).map(_.name)
  }

  private def methodName(am: AnnotatedMethod): Option[String] = {
    val d = _descriptorFor(am.getDeclaringClass)
    val getterSetter = d.properties.find(p => (p.getter ++ p.setter).exists(_ == am.getAnnotated)).map(_.name)
    getterSetter match {
      case Some(s) => Some(s)
      case _ => d.properties.find(p => p.name == am.getName).map(_.name)
    }
  }

  private def paramName(ap: AnnotatedParameter): Option[String] = {
    val d = _descriptorFor(ap.getDeclaringClass)
    d.properties.find(p => p.param.exists { cp =>
      cp.constructor == ap.getOwner.getAnnotated && cp.index == ap.getIndex
    }).map(_.name)
  }

  private def isScalaPackage(pkg: Option[Package]): Boolean =
    pkg.exists(_.getName.startsWith("scala."))

  private def isMaybeScalaBeanType(cls: Class[_]): Boolean =
    cls.hasSignature && !isScalaPackage(Option(cls.getPackage))

  private def isScala(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedClass => isMaybeScalaBeanType(ac.getAnnotated)
      case am: AnnotatedMember => isMaybeScalaBeanType(am.getDeclaringClass)
    }
  }

  def propertyFor(a: Annotated): Option[PropertyDescriptor] = {
    a match {
      case ap: AnnotatedParameter =>
        val d = _descriptorFor(ap.getDeclaringClass)
        d.properties.find(p =>
          p.param.exists { cp =>
            (cp.constructor == ap.getOwner.getAnnotated) && (cp.index == ap.getIndex)
          }
        )
      case am: AnnotatedMember =>
        val d = _descriptorFor(am.getDeclaringClass)
        d.properties.find(p =>
          (p.field ++ p.getter ++ p.setter ++ p.param ++ p.beanGetter ++ p.beanSetter).exists(_  == a.getAnnotated)
        )
    }
  }

  override def findImplicitPropertyName(mapperConfig: MapperConfig[_], member: AnnotatedMember): String = {
    member match {
      case af: AnnotatedField => fieldName(af).orNull
      case am: AnnotatedMethod => methodName(am).orNull
      case ap: AnnotatedParameter => paramName(ap).orNull
      case _ => None.orNull
    }
  }

  def hasCreatorAnnotation(a: Annotated): Boolean = {
    val jsonCreators: PartialFunction[Annotation, JsonCreator] = { case jc: JsonCreator => jc }

    a match {
      case ac: AnnotatedConstructor =>
        if (!isScala(ac)) return false
        val annotatedFound = _descriptorFor(ac.getDeclaringClass)
          .properties
          .flatMap(_.param)
          .exists(_.constructor == ac.getAnnotated)

        // Ignore this annotation if there is another annotation that is actually annotated with @JsonCreator.
        val annotatedConstructor = {
          for (constructor <- ac.getDeclaringClass.getDeclaredConstructors;
               annotation: JsonCreator <- constructor.getAnnotations.collect(jsonCreators) if annotation.mode() != JsonCreator.Mode.DISABLED) yield constructor
        }.headOption

        // Ignore this annotation if it is Mode.DISABLED.
        val isDisabled = ac.getAnnotated.getAnnotations.collect(jsonCreators).exists(_.mode() == JsonCreator.Mode.DISABLED)

        annotatedFound && annotatedConstructor.forall(_ == ac.getAnnotated) && !isDisabled
      case _ => false
    }
  }

  def findCreatorBinding(a: Annotated): JsonCreator.Mode = {
    val ann = _findAnnotation(a, classOf[JsonCreator])
    if (ann != null) {
      ann.mode()
    } else if (isScala(a) && hasCreatorAnnotation(a)) {
      JsonCreator.Mode.PROPERTIES
    } else None.orNull
  }

  class ScalaValueInstantiator(delegate: StdValueInstantiator, config: DeserializationConfig, descriptor: BeanDescriptor) extends StdValueInstantiator(delegate) {

    private val overriddenConstructorArguments: Array[SettableBeanProperty] = {
      val args = delegate.getFromObjectArguments(config)
      Option(args) match {
        case Some(array) => {
          array.map {
            case creator: CreatorProperty =>
              // Locate the constructor param that matches it
              descriptor.properties.find(_.param.exists(_.index == creator.getCreatorIndex)) match {
                case Some(PropertyDescriptor(name, Some(ConstructorParameter(_, _, Some(defaultValue))), _, _, _, _, _)) =>
                  creator.withNullProvider(new NullValueProvider {
                    override def getNullValue(ctxt: DeserializationContext): AnyRef = defaultValue()

                    override def getNullAccessPattern: AccessPattern = AccessPattern.DYNAMIC
                  })
                case _ => creator
              }
            case other => other
          }
        }
        case _ => Array.empty
      }
    }

    override def getFromObjectArguments(config: DeserializationConfig): Array[SettableBeanProperty] = {
      overriddenConstructorArguments
    }
  }

  override def findValueInstantiator(config: DeserializationConfig, beanDesc: BeanDescription,
    defaultInstantiator: ValueInstantiator): ValueInstantiator = {

    if (isMaybeScalaBeanType(beanDesc.getBeanClass)) {

      val descriptor = _descriptorFor(beanDesc.getBeanClass)
      if (descriptor.properties.exists(_.param.exists(_.defaultValue.isDefined))) {
        defaultInstantiator match {
          case std: StdValueInstantiator =>
            new ScalaValueInstantiator(std, config, descriptor)
          case other =>
            throw new IllegalArgumentException("Cannot customise a non StdValueInstantiatiator: " + other.getClass)
        }
      } else defaultInstantiator

    } else defaultInstantiator
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(JavaAnnotationIntrospector) }
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
  this += { _.addValueInstantiators(ScalaAnnotationIntrospector) }
}
