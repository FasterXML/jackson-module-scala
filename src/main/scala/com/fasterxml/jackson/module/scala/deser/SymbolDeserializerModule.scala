package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.languageFeature.postfixOps

private object SymbolDeserializer extends StdDeserializer[Symbol](classOf[Symbol]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Symbol =
    Symbol(p.getValueAsString)
}

private object SymbolDeserializerResolver extends Deserializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolDeserializer
    else null
}

trait SymbolDeserializerModule extends JacksonModule {
  override def getModuleName: String = "SymbolDeserializerModule"
  this += { _ addDeserializers SymbolDeserializerResolver }
}
