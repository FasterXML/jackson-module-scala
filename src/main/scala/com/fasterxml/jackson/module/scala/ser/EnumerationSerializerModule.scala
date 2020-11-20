package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.ser.{ContextualSerializer, Serializers}
import com.fasterxml.jackson.module.scala.util.Implicits._
import com.fasterxml.jackson.module.scala.{JacksonModule, JsonScalaEnumeration}

trait ContextualEnumerationSerializer extends ContextualSerializer
{
  self: JsonSerializer[_] =>

  override def createContextual(serializerProvider: SerializerProvider, beanProperty: BeanProperty): JsonSerializer[_] =
    Option(beanProperty)
      .optMap(_.getAnnotation(classOf[JsonScalaEnumeration]))
      .map(_ => new AnnotatedEnumerationSerializer)
      .getOrElse(this)
}

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
private class EnumerationSerializer extends JsonSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider) = {
    val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
    val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
    jgen.writeStartObject()
    jgen.writeStringField("enumClass", enumClass)
    jgen.writeStringField("value", value.toString)
    jgen.writeEndObject()
  }
}

private class AnnotatedEnumerationSerializer extends JsonSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeString(value.toString)
  }
}

private object EnumerationSerializerResolver extends Serializers.Base {

  private val EnumClass = classOf[scala.Enumeration#Value]

  override def findSerializer(config: SerializationConfig,
                              javaType: JavaType,
                              beanDescription: BeanDescription): JsonSerializer[_] = {
    val clazz = javaType.getRawClass

    if (EnumClass.isAssignableFrom(clazz)) {
      new EnumerationSerializer
    } else {
      None.orNull
    }
  }

}

trait EnumerationSerializerModule extends JacksonModule {
  this += EnumerationSerializerResolver
}
