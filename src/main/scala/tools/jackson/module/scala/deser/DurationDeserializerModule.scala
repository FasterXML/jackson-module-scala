package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.SimpleType
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.deser.{Deserializers, KeyDeserializers}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.DurationConverters
import tools.jackson.module.scala.{JacksonModule, ScalaModule}

import java.time.{Duration => JavaDuration}
import scala.concurrent.duration.FiniteDuration

private object FiniteDurationDeserializerShared {
  private[deser] val JavaDurationClass = classOf[JavaDuration]
  private[deser] lazy val JavaDurationType = SimpleType.constructUnsafe(JavaDurationClass)
  private[deser] val FiniteDurationClass = classOf[FiniteDuration]
}

// databind reads the Java duration, so whatever it accepts - a timestamp, an ISO-8601 period - is
// what a FiniteDuration can be read from.
private object FiniteDurationDeserializer extends StdDeserializer[FiniteDuration](classOf[FiniteDuration]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): FiniteDuration = {
    Option(ctxt.readValue(p, FiniteDurationDeserializerShared.JavaDurationClass)) match {
      case Some(duration) => DurationConverters.toScala(duration)
      case _ => None.orNull
    }
  }
}

private object FiniteDurationKeyDeserializer extends KeyDeserializer {
  override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef = {
    val keyDeserializer = ctxt.findKeyDeserializer(FiniteDurationDeserializerShared.JavaDurationType, None.orNull)
    DurationConverters.toScala(keyDeserializer.deserializeKey(key, ctxt).asInstanceOf[JavaDuration])
  }
}

private class FiniteDurationDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig,
                                    beanDesc: BeanDescription.Supplier): ValueDeserializer[FiniteDuration] =
    if (FiniteDurationDeserializerShared.FiniteDurationClass.isAssignableFrom(javaType.getRawClass))
      FiniteDurationDeserializer
    else None.orNull

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean =
    FiniteDurationDeserializerShared.FiniteDurationClass.isAssignableFrom(valueType)
}

private class FiniteDurationKeyDeserializerResolver(config: ScalaModule.Config) extends KeyDeserializers {
  override def findKeyDeserializer(javaType: JavaType, deserializationConfig: DeserializationConfig,
                                   beanDesc: BeanDescription.Supplier): KeyDeserializer =
    if (FiniteDurationDeserializerShared.FiniteDurationClass.isAssignableFrom(javaType.getRawClass))
      FiniteDurationKeyDeserializer
    else None.orNull
}

trait DurationDeserializerModule extends JacksonModule {
  override def getModuleName: String = "DurationDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new FiniteDurationDeserializerResolver(config)
    builder += new FiniteDurationKeyDeserializerResolver(config)
    builder.build()
  }
}

object DurationDeserializerModule extends DurationDeserializerModule
