package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder
import com.fasterxml.jackson.module.scala.util.ClassW

import scala.languageFeature.postfixOps

private class ScalaObjectDeserializer(clazz: Class[_]) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = {
    clazz.getDeclaredFields.find(_.getName == "MODULE$").map(_.get(null)).getOrElse(null)
  }
}

private class ScalaObjectDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig, beanDesc: BeanDescription): ValueDeserializer[_] = {
    val clazz = javaType.getRawClass
    if (hasDeserializerFor(deserializationConfig, clazz))
      new ScalaObjectDeserializer(clazz)
    else null
  }

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    ClassW(valueType).isScalaObject
  }
}

trait ScalaObjectDeserializerModule extends JacksonModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new ScalaObjectDeserializerResolver(config)
    builder.build()
  }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
