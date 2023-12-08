package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.KeyDeserializers
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.languageFeature.postfixOps
import scala.reflect.Enum

private object EnumSerializerShared {
  val EnumClass = classOf[Enum]
}

private object EnumSerializer extends JsonSerializer[Enum] {
  def serialize(value: Enum, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    provider.defaultSerializeValue(value.toString, jgen)
}

private object EnumKeySerializer extends JsonSerializer[Enum] {
  def serialize(value: Enum, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeFieldName(value.toString)
}

private object EnumSerializerResolver extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[Enum] =
    if (EnumSerializerShared.EnumClass.isAssignableFrom(javaType.getRawClass))
      EnumSerializer
    else None.orNull
}

private object EnumKeySerializerResolver extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[Enum] =
    if (EnumSerializerShared.EnumClass isAssignableFrom javaType.getRawClass)
      EnumKeySerializer
    else None.orNull
}

trait EnumSerializerModule extends JacksonModule {
  override def getModuleName: String = "EnumSerializerModule"
  this += { _ addSerializers EnumSerializerResolver }
  this += { _ addKeySerializers EnumKeySerializerResolver }
}
