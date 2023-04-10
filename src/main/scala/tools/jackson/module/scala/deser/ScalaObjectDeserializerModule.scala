package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.ClassW

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
  override def getModuleName: String = "ScalaObjectDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new ScalaObjectDeserializerResolver(config)
    builder.build()
  }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
