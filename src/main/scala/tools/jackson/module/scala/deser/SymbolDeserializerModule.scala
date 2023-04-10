package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private object SymbolDeserializer extends StdDeserializer[Symbol](classOf[Symbol]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Symbol =
    Symbol(p.getValueAsString)
}

private class SymbolDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findBeanDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig, beanDesc: BeanDescription): ValueDeserializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolDeserializer
    else null

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    SYMBOL isAssignableFrom valueType
  }

}

trait SymbolDeserializerModule extends JacksonModule {
  override def getModuleName: String = "SymbolDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SymbolDeserializerResolver(config)
    builder.build()
  }
}

object SymbolDeserializerModule extends SymbolDeserializerModule
