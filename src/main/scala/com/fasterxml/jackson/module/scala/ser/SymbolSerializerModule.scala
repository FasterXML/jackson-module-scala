package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private object SymbolSerializer extends ValueSerializer[Symbol] {
  def serialize(value: Symbol, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.name)
}

private class SymbolSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findSerializer(serializationConfig: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolSerializer
    else null
}

trait SymbolSerializerModule extends JacksonModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    this += {
      _ addSerializers new SymbolSerializerResolver(config)
    }
  }
}
