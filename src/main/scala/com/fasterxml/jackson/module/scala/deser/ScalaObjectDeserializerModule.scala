package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ScalaObject

import scala.languageFeature.postfixOps

private class ScalaObjectDeserializer(scalaObject: Any) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = scalaObject
}

private object ScalaObjectDeserializerResolver extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    val clazz = javaType.getRawClass
    clazz match {
      case ScalaObject(value) => new ScalaObjectDeserializer(value)
      case _ => null
    }
  }
}

trait ScalaObjectDeserializerModule extends JacksonModule {
  override def getModuleName: String = "ScalaObjectDeserializerModule"

  this += {
    _ addDeserializers ScalaObjectDeserializerResolver
  }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
