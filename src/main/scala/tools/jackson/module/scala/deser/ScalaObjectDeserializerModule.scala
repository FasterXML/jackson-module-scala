package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.ClassW

import scala.languageFeature.postfixOps

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

private class ScalaObjectDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig, beanDesc: BeanDescription): ValueDeserializer[_] = {
    ClassW(javaType.getRawClass).getModuleField.flatMap { field =>
      Option(field.get(null))
    }.map(new ScalaObjectDeserializer(_)).orNull
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
