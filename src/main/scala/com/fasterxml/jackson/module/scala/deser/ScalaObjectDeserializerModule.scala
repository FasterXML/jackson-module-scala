package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ClassW

import scala.languageFeature.postfixOps
import scala.util.control.NonFatal

private class ScalaObjectDeserializer(clazz: Class[_]) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = {
    try {
      clazz.getField("MODULE$").get(null)
    } catch {
      case NonFatal(_) => null
    }
  }
}

private object ScalaObjectDeserializerResolver extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    val clazz = javaType.getRawClass
    if (ClassW(clazz).isScalaObject)
      new ScalaObjectDeserializer(clazz)
    else null
  }
}

trait ScalaObjectDeserializerModule extends JacksonModule {
  override def getModuleName: String = "ScalaObjectDeserializerModule"
  this += { _ addDeserializers ScalaObjectDeserializerResolver }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
