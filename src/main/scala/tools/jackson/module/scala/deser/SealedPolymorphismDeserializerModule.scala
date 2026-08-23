package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.deser.{BeanDeserializerBuilder, Deserializers, ValueDeserializerModifier}
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.SealedPolymorphism

/** Buffers a tagged object, pulling the `@type` property out of the tokens handed on. */
private object TaggedObject {
  def split(p: JsonParser, ctxt: DeserializationContext): (String, JsonParser) = {
    val buffer = ctxt.bufferForInputBuffering(p)
    buffer.writeStartObject()
    var typeName: String = None.orNull
    while (p.nextToken() != JsonToken.END_OBJECT) {
      val name = p.currentName()
      p.nextToken()
      if (typeName == null && name == SealedPolymorphism.TypePropertyName) {
        typeName = p.getValueAsString
      } else {
        buffer.writeName(name)
        buffer.copyCurrentStructure(p)
      }
    }
    buffer.writeEndObject()
    (typeName, buffer.asParserOnFirstToken(ctxt))
  }

  def failed(baseClass: Class[_], typeName: String): Nothing =
    throw new IllegalArgumentException(s"Failed to create ${baseClass.getName} instance for $typeName")
}

/**
 * Reads a value of a hierarchy marked with [[tools.jackson.module.scala.SealedPolymorphismSupport]]
 * by dispatching on its `@type` property. Bound to a trait or abstract class, which never holds a
 * value of its own.
 */
private case class SealedPolymorphicDeserializer(baseClass: Class[_]) extends StdDeserializer[AnyRef](baseClass) {

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (p.currentToken() != JsonToken.START_OBJECT) {
      throw new IllegalArgumentException(
        s"Expected a JSON object with a ${SealedPolymorphism.TypePropertyName} property to create ${baseClass.getName}")
    }
    val (typeName, buffered) = TaggedObject.split(p, ctxt)
    val subtype = Option(typeName).flatMap(SealedPolymorphism.resolve(baseClass, _))
      .getOrElse(TaggedObject.failed(baseClass, typeName))
    subtype.singleton match {
      case Some(instance) => instance
      case None => ctxt.readValue(buffered, subtype.clazz.asInstanceOf[Class[AnyRef]])
    }
  }
}

/**
 * Reads a value at a concrete type that others may extend. Such a type is both a value in its own
 * right and a base its subclasses are read through, so this wraps that type's own bean
 * deserializer: a `@type` naming it, or no `@type` at all, reads through to the delegate, and
 * anything else is dispatched. Wrapping rather than replacing is what lets the type be read as
 * itself without recursing.
 */
private class TaggedBeanDeserializer(declaredClass: Class[_], delegate: ValueDeserializer[AnyRef])
  extends ValueDeserializer[AnyRef] {

  override def resolve(ctxt: DeserializationContext): Unit = delegate.resolve(ctxt)

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): ValueDeserializer[_] = {
    val contextual = delegate.createContextual(ctxt, property)
    if (contextual eq delegate) this
    else new TaggedBeanDeserializer(declaredClass, contextual.asInstanceOf[ValueDeserializer[AnyRef]])
  }

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (p.currentToken() != JsonToken.START_OBJECT) delegate.deserialize(p, ctxt)
    else {
      val (typeName, buffered) = TaggedObject.split(p, ctxt)
      // an untagged object is simply a value of the type it was declared as
      if (typeName == null) delegate.deserialize(buffered, ctxt)
      else SealedPolymorphism.resolve(declaredClass, typeName) match {
        case Some(subtype) if subtype.clazz == declaredClass => delegate.deserialize(buffered, ctxt)
        case Some(subtype) =>
          subtype.singleton.getOrElse(ctxt.readValue(buffered, subtype.clazz.asInstanceOf[Class[AnyRef]]))
        case None => TaggedObject.failed(declaredClass, typeName)
      }
    }
  }
}

private class SealedPolymorphismDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[AnyRef] =
    if (hasDeserializerFor(config, javaType.getRawClass)) SealedPolymorphicDeserializer(javaType.getRawClass)
    else None.orNull

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean =
    SealedPolymorphism.isBaseType(valueType)
}

/**
 * The dispatching deserializer strips `@type` before delegating, but a property may also be declared
 * as the implementation type rather than the base type, in which case the value is read straight as
 * a bean and has to tolerate the tag.
 */
private class SealedPolymorphismDeserializerModifier(config: ScalaModule.Config) extends ValueDeserializerModifier {
  override def updateBuilder(config: DeserializationConfig, beanDesc: BeanDescription.Supplier,
                             builder: BeanDeserializerBuilder): BeanDeserializerBuilder = {
    val rawClass = beanDesc.getBeanClass
    if (SealedPolymorphism.conflictingJsonTypeInfo(rawClass)) {
      throw new IllegalArgumentException(SealedPolymorphism.conflictMessage(rawClass))
    }
    if (SealedPolymorphism.isSupported(rawClass) && !SealedPolymorphism.isBaseType(rawClass)) {
      builder.addIgnorable(SealedPolymorphism.TypePropertyName)
    }
    builder
  }

  override def modifyDeserializer(config: DeserializationConfig, beanDesc: BeanDescription.Supplier,
                                  deserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
    val rawClass = beanDesc.getBeanClass
    if (SealedPolymorphism.needsSubtypeDispatch(rawClass)) {
      new TaggedBeanDeserializer(rawClass, deserializer.asInstanceOf[ValueDeserializer[AnyRef]])
    } else deserializer
  }
}

trait SealedPolymorphismDeserializerModule extends JacksonModule {
  override def getModuleName: String = "SealedPolymorphismDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SealedPolymorphismDeserializerResolver(config)
    builder += new SealedPolymorphismDeserializerModifier(config)
    builder.build()
  }
}

object SealedPolymorphismDeserializerModule extends SealedPolymorphismDeserializerModule
