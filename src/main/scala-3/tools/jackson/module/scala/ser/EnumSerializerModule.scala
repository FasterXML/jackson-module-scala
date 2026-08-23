package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind._
import tools.jackson.databind.deser.KeyDeserializers
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.deser.EnumDeserializerShared

import scala.languageFeature.postfixOps
import scala.reflect.Enum

private object EnumSerializer extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit =
    jgen.writeString(value.toString)

}

private object EnumKeySerializer extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit =
    jgen.writeName(value.toString)
}

private class EnumSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Enum] =
    if (EnumDeserializerShared.EnumClass.isAssignableFrom(javaType.getRawClass)
        && EnumDeserializerShared.canFindByOrdinal(javaType.getRawClass))
      EnumSerializer
    else None.orNull
}

private class EnumKeySerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Enum] =
    if (EnumDeserializerShared.EnumClass isAssignableFrom javaType.getRawClass)
      EnumKeySerializer
    else None.orNull
}

trait EnumSerializerModule extends JacksonModule {
  override def getModuleName: String = "EnumSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumSerializerResolver(config)
    builder.addKeySerializers(new EnumKeySerializerResolver(config))
    builder.build()
  }
}

object EnumSerializerModule extends EnumSerializerModule
