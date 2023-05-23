package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.languageFeature.postfixOps

private object SymbolSerializer extends JsonSerializer[Symbol] {
  def serialize(value: Symbol, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.name)
}

private object SymbolSerializerResolver extends Serializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolSerializer
    else None.orNull
}

trait SymbolSerializerModule extends JacksonModule {
  override def getModuleName: String = "SymbolSerializerModule"
  this += { _ addSerializers SymbolSerializerResolver }
}
