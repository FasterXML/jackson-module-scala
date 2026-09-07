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

private class ScalaObjectDeserializer(value: Any) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = {
    // A Scala object holds no state, so whatever was written for it is consumed and discarded.
    // skipChildren stops at the end token matching the one it started on - not at the first one it
    // meets - and does nothing at all for a scalar, so a nested object no longer ends the value
    // early and a scalar no longer eats the tokens of whatever encloses it. Reading a scalar at the
    // root used to spin forever here: nextToken returns null at end of input, never END_OBJECT.
    p.skipChildren()
    value
  }
}

private class ScalaObjectDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[_] = {
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
