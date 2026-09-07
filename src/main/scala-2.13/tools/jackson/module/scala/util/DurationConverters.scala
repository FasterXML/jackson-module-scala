package tools.jackson.module.scala.util

import java.time.{Duration => JavaDuration}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.{DurationConverters => JdkDurationConverters}

/**
 * Converts between a Scala [[FiniteDuration]] and a Java [[java.time.Duration]].
 *
 * Scala 2.13 added these to the standard library, so there is nothing to do here but call them.
 * The 2.12 build carries its own copy of the same conversions.
 */
private[scala] object DurationConverters {

  def toScala(duration: JavaDuration): FiniteDuration =
    JdkDurationConverters.JavaDurationOps(duration).toScala

  def toJava(duration: FiniteDuration): JavaDuration =
    JdkDurationConverters.ScalaDurationOps(duration).toJava
}
