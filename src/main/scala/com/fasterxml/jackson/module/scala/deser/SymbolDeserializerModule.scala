package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private object SymbolDeserializer extends StdDeserializer[Symbol](classOf[Symbol]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Symbol =
    Symbol(p.getValueAsString)
}

private class SymbolDeserializerResolver(builder: ScalaModule.ReadOnlyBuilder = ScalaModule.defaultBuilder) extends Deserializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): ValueDeserializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolDeserializer
    else null

  override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
    SYMBOL isAssignableFrom valueType
  }

}

trait SymbolDeserializerModule extends JacksonModule {
  this += { _ addDeserializers new SymbolDeserializerResolver() }
}
