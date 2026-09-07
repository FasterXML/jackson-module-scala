package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.DurationDeserializerModule
import tools.jackson.module.scala.ser.DurationSerializerModule

/**
 * Adds support for `scala.concurrent.duration.FiniteDuration`, as a value and as a map key.
 *
 * A FiniteDuration is converted to and from a `java.time.Duration`, which databind then reads and
 * writes - so a duration is formatted by whatever the mapper's `DateTimeFeature` settings say, as a
 * timestamp or as an ISO-8601 period, exactly as a Java duration would be.
 *
 * Without this, a FiniteDuration is written as whatever its fields happen to be in the Scala version
 * in use - `{"length":7,"unit":"DAYS","finite":true}` - which differs between releases and cannot
 * reliably be read back.
 *
 * Derived from https://github.com/pjfanning/jackson-module-scala-duration.
 *
 * @since 3.3.0
 */
trait DurationModule extends DurationSerializerModule with DurationDeserializerModule {
  override def getModuleName: String = "DurationModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    DurationSerializerModule.getInitializers(config) ++
      DurationDeserializerModule.getInitializers(config)
  }
}

object DurationModule extends DurationModule
