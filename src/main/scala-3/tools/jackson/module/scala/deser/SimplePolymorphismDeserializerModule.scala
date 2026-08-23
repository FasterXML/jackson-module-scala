package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.deser.{BeanDeserializerBuilder, Deserializers, ValueDeserializerModifier}
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.SimplePolymorphism

/**
 * Reads a value of a hierarchy marked with [[tools.jackson.module.scala.SimplePolymorphismSupport]]
 * by dispatching on its `@type` property.
 */
private case class SimplePolymorphicDeserializer(baseClass: Class[_]) extends StdDeserializer[AnyRef](baseClass) {

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): AnyRef = {
    if (p.currentToken() != JsonToken.START_OBJECT) {
      throw new IllegalArgumentException(
        s"Expected a JSON object with a ${SimplePolymorphism.TypePropertyName} property to create ${baseClass.getName}")
    }
    val buffer = ctxt.bufferForInputBuffering(p)
    buffer.writeStartObject()
    var typeName: String = None.orNull
    while (p.nextToken() != JsonToken.END_OBJECT) {
      val name = p.currentName()
      p.nextToken()
      if (typeName == null && name == SimplePolymorphism.TypePropertyName) {
        typeName = p.getValueAsString
      } else {
        buffer.writeName(name)
        buffer.copyCurrentStructure(p)
      }
    }
    buffer.writeEndObject()
    val subtype = Option(typeName).flatMap(SimplePolymorphism.resolve(baseClass, _)).getOrElse(failed(typeName))
    subtype.singleton match {
      case Some(instance) => instance
      case None => ctxt.readValue(buffer.asParserOnFirstToken(ctxt), subtype.clazz.asInstanceOf[Class[AnyRef]])
    }
  }

  private def failed(typeName: String): Nothing =
    throw new IllegalArgumentException(s"Failed to create ${baseClass.getName} instance for $typeName")
}

private class SimplePolymorphismDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[AnyRef] =
    if (hasDeserializerFor(config, javaType.getRawClass)) SimplePolymorphicDeserializer(javaType.getRawClass)
    else None.orNull

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean =
    SimplePolymorphism.isBaseType(valueType)
}

/**
 * The dispatching deserializer strips `@type` before delegating, but a property may also be declared
 * as the implementation type rather than the base type, in which case the value is read straight as
 * a bean and has to tolerate the tag.
 */
private class SimplePolymorphismDeserializerModifier(config: ScalaModule.Config) extends ValueDeserializerModifier {
  override def updateBuilder(config: DeserializationConfig, beanDesc: BeanDescription.Supplier,
                             builder: BeanDeserializerBuilder): BeanDeserializerBuilder = {
    val rawClass = beanDesc.getBeanClass
    if (SimplePolymorphism.conflictingJsonTypeInfo(rawClass)) {
      throw new IllegalArgumentException(SimplePolymorphism.conflictMessage(rawClass))
    }
    if (SimplePolymorphism.isSupported(rawClass) && !SimplePolymorphism.isBaseType(rawClass)) {
      builder.addIgnorable(SimplePolymorphism.TypePropertyName)
    }
    builder
  }
}

trait SimplePolymorphismDeserializerModule extends JacksonModule {
  override def getModuleName: String = "SimplePolymorphismDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SimplePolymorphismDeserializerResolver(config)
    builder += new SimplePolymorphismDeserializerModifier(config)
    builder.build()
  }
}

object SimplePolymorphismDeserializerModule extends SimplePolymorphismDeserializerModule
