package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.ser.Serializers
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private object SymbolSerializer extends ValueSerializer[Symbol] {
  def serialize(value: Symbol, jgen: JsonGenerator, provider: SerializationContext): Unit =
    jgen.writeString(value.name)
}

private class SymbolSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  private val SYMBOL = classOf[Symbol]

  override def findSerializer(serializationConfig: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Symbol] =
    if (SYMBOL isAssignableFrom javaType.getRawClass)
      SymbolSerializer
    else null
}

trait SymbolSerializerModule extends JacksonModule {
  override def getModuleName: String = "SymbolSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new SymbolSerializerResolver(config)
    builder.build()
  }
}

object SymbolSerializerModule extends SymbolSerializerModule
