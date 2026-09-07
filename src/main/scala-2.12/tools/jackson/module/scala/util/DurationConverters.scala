package tools.jackson.module.scala.util

import java.time.temporal.ChronoUnit
import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Converts between a Scala [[FiniteDuration]] and a Java [[java.time.Duration]].
 *
 * Scala 2.13 added these to the standard library as `scala.jdk.DurationConverters`, which the 2.13
 * and Scala 3 builds call instead of this. Scala 2.12 has no `scala.jdk` package at all - there the
 * conversions live in scala-java8-compat, a dependency this module does not want for two methods,
 * so just those two are copied from `scala.jdk.javaapi.DurationConverters` (Apache 2.0, Copyright
 * EPFL and Lightbend, Inc.).
 *
 * Copied rather than rewritten: the same overflow check, the same exception, the same answers. The
 * tests run on all three builds, so this is held to what the standard library actually does.
 */
private[scala] object DurationConverters {

  def toJava(duration: FiniteDuration): JavaDuration = {
    if (duration.length == 0) JavaDuration.ZERO
    else duration.unit match {
      case TimeUnit.NANOSECONDS => JavaDuration.ofNanos(duration.length)
      case TimeUnit.MICROSECONDS => JavaDuration.of(duration.length, ChronoUnit.MICROS)
      case TimeUnit.MILLISECONDS => JavaDuration.ofMillis(duration.length)
      case TimeUnit.SECONDS => JavaDuration.ofSeconds(duration.length)
      case TimeUnit.MINUTES => JavaDuration.ofMinutes(duration.length)
      case TimeUnit.HOURS => JavaDuration.ofHours(duration.length)
      case TimeUnit.DAYS => JavaDuration.ofDays(duration.length)
    }
  }

  def toScala(duration: JavaDuration): FiniteDuration = {
    val originalSeconds = duration.getSeconds
    val originalNanos = duration.getNano
    if (originalNanos == 0) {
      if (originalSeconds == 0) Duration.Zero else FiniteDuration(originalSeconds, TimeUnit.SECONDS)
    } else if (originalSeconds == 0) {
      FiniteDuration(originalNanos.toLong, TimeUnit.NANOSECONDS)
    } else {
      try {
        val secondsAsNanos = Math.multiplyExact(originalSeconds, 1000000000L)
        val totalNanos = secondsAsNanos + originalNanos
        if ((totalNanos < 0 && secondsAsNanos < 0) || (totalNanos > 0 && secondsAsNanos > 0)) {
          FiniteDuration(totalNanos, TimeUnit.NANOSECONDS)
        } else {
          throw new ArithmeticException()
        }
      } catch {
        case _: ArithmeticException =>
          throw new IllegalArgumentException(s"Java duration $duration cannot be expressed as a Scala duration")
      }
    }
  }
}
