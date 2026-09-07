package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.ser.Serializers
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.DurationConverters
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import java.time.{Duration => JavaDuration}
import scala.concurrent.duration.FiniteDuration

private object FiniteDurationSerializerShared {
  private[ser] val FiniteDurationClass = classOf[FiniteDuration]
  private[ser] val JavaDurationClass = classOf[JavaDuration]
}

// The Java duration is handed to databind rather than written here, so a duration is formatted by
// whatever the mapper's DateTimeFeature settings say - as a timestamp, or as an ISO-8601 period.
private object FiniteDurationSerializer extends ValueSerializer[FiniteDuration] {
  override def serialize(value: FiniteDuration, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    serializationContext.writeValue(jgen, DurationConverters.toJava(value))
  }
}

private object FiniteDurationKeySerializer extends ValueSerializer[FiniteDuration] {
  override def serialize(value: FiniteDuration, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    val keySerializer = serializationContext.findKeySerializer(FiniteDurationSerializerShared.JavaDurationClass, None.orNull)
    keySerializer.serialize(DurationConverters.toJava(value), jgen, serializationContext)
  }
}

private class FiniteDurationSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(serializationConfig: SerializationConfig, javaType: JavaType,
                              beanDesc: BeanDescription.Supplier, formatOverrides: JsonFormat.Value): ValueSerializer[FiniteDuration] =
    if (FiniteDurationSerializerShared.FiniteDurationClass.isAssignableFrom(javaType.getRawClass))
      FiniteDurationSerializer
    else None.orNull
}

private class FiniteDurationKeySerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(serializationConfig: SerializationConfig, javaType: JavaType,
                              beanDesc: BeanDescription.Supplier, formatOverrides: JsonFormat.Value): ValueSerializer[FiniteDuration] =
    if (FiniteDurationSerializerShared.FiniteDurationClass.isAssignableFrom(javaType.getRawClass))
      FiniteDurationKeySerializer
    else None.orNull
}

trait DurationSerializerModule extends JacksonModule {
  override def getModuleName: String = "DurationSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new FiniteDurationSerializerResolver(config)
    builder.addKeySerializers(new FiniteDurationKeySerializerResolver(config))
    builder.build()
  }
}

object DurationSerializerModule extends DurationSerializerModule
