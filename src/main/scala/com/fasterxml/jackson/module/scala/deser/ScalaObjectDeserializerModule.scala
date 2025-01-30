package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ClassW

import scala.languageFeature.postfixOps
import scala.util.control.NonFatal

private class ScalaObjectDeserializer(value: Any) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = {
    if (p.currentToken() != JsonToken.END_OBJECT) {
      while (p.nextToken() != JsonToken.END_OBJECT) {
        // consume the object
      }
    }
    value
  }
}

private object ScalaObjectDeserializerResolver extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    ClassW(javaType.getRawClass).getModuleField.flatMap { field =>
      Option(field.get(null))
    }.map(new ScalaObjectDeserializer(_)).orNull
  }
}

trait ScalaObjectDeserializerModule extends JacksonModule {
  override def getModuleName: String = "ScalaObjectDeserializerModule"
  this += { _ addDeserializers ScalaObjectDeserializerResolver }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
