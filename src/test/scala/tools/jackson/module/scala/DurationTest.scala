package tools.jackson.module.scala

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.util.DurationConverters

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration}

object DurationTest {
  case class DurationWrapper(duration: FiniteDuration)
}

/**
 * Ported from https://github.com/pjfanning/jackson-module-scala-duration, whose DurationModule this
 * module now carries.
 */
class DurationTest extends AnyWordSpec with Matchers {

  import DurationTest._

  private val week = DurationWrapper(7.days)
  private val second = DurationWrapper(1.second)

  private def mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def mapperWith(feature: DateTimeFeature, enabled: Boolean) = {
    val builder = JsonMapper.builder().addModule(DefaultScalaModule)
    if (enabled) builder.enable(feature) else builder.disable(feature)
    builder.build()
  }

  "DurationModule" should {
    "write a duration as a period" in {
      mapper.writeValueAsString(week) shouldEqual """{"duration":"PT168H"}"""
      mapper.writeValueAsString(second) shouldEqual """{"duration":"PT1S"}"""
    }
    "write a duration as a timestamp" in {
      val timestamps = mapperWith(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS, enabled = true)
      timestamps.writeValueAsString(week) shouldEqual """{"duration":604800.000000000}"""
      timestamps.writeValueAsString(second) shouldEqual """{"duration":1.000000000}"""
    }
    "read a duration written as a timestamp" in {
      mapper.readValue("""{"duration":604800}""", classOf[DurationWrapper]) shouldEqual week
      mapper.readValue("""{"duration":604800.0000}""", classOf[DurationWrapper]) shouldEqual week
    }
    "read a duration written as a period" in {
      mapper.readValue("""{"duration":"PT168H"}""", classOf[DurationWrapper]) shouldEqual week
      mapper.readValue("""{"duration":"P7D"}""", classOf[DurationWrapper]) shouldEqual week
    }
    "round trip a duration" in {
      val read = mapper.readValue(mapper.writeValueAsString(week), classOf[DurationWrapper])
      read shouldEqual week
    }
    "write a duration used as a map key" in {
      mapper.writeValueAsString(Map(second.duration -> "mapped")) shouldEqual """{"PT1S":"mapped"}"""
    }
    "read a duration used as a map key" in {
      val map = mapper.readValue("""{"PT1S":"mapped"}""", new TypeReference[Map[FiniteDuration, String]] {})
      map should have size 1
      map(1.second) shouldEqual "mapped"
    }
    "be part of a module built by the builder" in {
      val built = JsonMapper.builder()
        .addModule(ScalaModule.builder().addAllBuiltinModules().build())
        .build()
      built.writeValueAsString(week) shouldEqual """{"duration":"PT168H"}"""
      built.readValue("""{"duration":"PT168H"}""", classOf[DurationWrapper]) shouldEqual week
    }
    "carry a duration finer than a second through JSON" in {
      val nanos = DurationWrapper(1500000001.nanos)
      mapper.readValue(mapper.writeValueAsString(nanos), classOf[DurationWrapper]) shouldEqual nanos
      mapper.writeValueAsString(DurationWrapper(1500.millis)) shouldEqual """{"duration":"PT1.5S"}"""
    }
    "be usable on its own, without the rest of the module" in {
      val alone = JsonMapper.builder().addModule(DurationModule).build()
      alone.writeValueAsString(7.days) shouldEqual """"PT168H""""
      alone.readValue(""""PT168H"""", classOf[FiniteDuration]) shouldEqual 7.days
    }
  }

  // The 2.13 and Scala 3 builds delegate to scala.jdk.DurationConverters; 2.12 has no such thing in
  // its standard library - it has no scala.jdk package at all - so that build carries its own copy.
  // Every case below runs on all three, so the copy is held to exactly what the standard library
  // answers rather than to what it was written to answer.
  private val equivalents: Seq[(FiniteDuration, JavaDuration)] = Seq(
    Duration.Zero -> JavaDuration.ZERO,
    1.nano -> JavaDuration.ofNanos(1),
    999999999.nanos -> JavaDuration.ofNanos(999999999),
    1.micro -> JavaDuration.ofNanos(1000),
    1.milli -> JavaDuration.ofMillis(1),
    1.second -> JavaDuration.ofSeconds(1),
    1.minute -> JavaDuration.ofMinutes(1),
    1.hour -> JavaDuration.ofHours(1),
    1.day -> JavaDuration.ofDays(1),
    7.days -> JavaDuration.ofDays(7),
    // a whole second carried alongside a fraction of one, which is the case the two halves of the
    // conversion have to agree about
    1500000000L.nanos -> JavaDuration.ofSeconds(1, 500000000),
    2500.millis -> JavaDuration.ofSeconds(2, 500000000),
    // java.time normalises a negative duration to a negative second count and a positive nano
    // remainder, so these are not simply the positive cases with a sign
    (-1).nano -> JavaDuration.ofNanos(-1),
    (-1500000000L).nanos -> JavaDuration.ofSeconds(-2, 500000000),
    (-7).days -> JavaDuration.ofDays(-7),
    // the largest a FiniteDuration goes
    FiniteDuration(Long.MaxValue, TimeUnit.NANOSECONDS) -> JavaDuration.ofNanos(Long.MaxValue)
  )

  "DurationConverters" should {
    "convert a Scala duration to the Java duration it stands for" in {
      equivalents.foreach { case (scala, java) =>
        withClue(s"$scala: ") { DurationConverters.toJava(scala) shouldEqual java }
      }
    }
    "convert a Java duration to the Scala duration it stands for" in {
      equivalents.foreach { case (scala, java) =>
        withClue(s"$java: ") { DurationConverters.toScala(java) shouldEqual scala }
      }
    }
    "round trip every one of them" in {
      equivalents.foreach { case (scala, _) =>
        withClue(s"$scala: ") { DurationConverters.toScala(DurationConverters.toJava(scala)) shouldEqual scala }
      }
    }
    "keep the unit a Scala duration was expressed in out of the answer" in {
      // FiniteDuration compares by length, so these are equal whatever unit each side chose
      DurationConverters.toJava(1.second) shouldEqual DurationConverters.toJava(1000.millis)
      DurationConverters.toScala(JavaDuration.ofDays(7)) shouldEqual 604800.seconds
    }
    "refuse a Java duration whose seconds alone overflow a FiniteDuration" in {
      an[IllegalArgumentException] should be thrownBy
        DurationConverters.toScala(JavaDuration.ofSeconds(Long.MaxValue, 1))
    }
    "refuse a Java duration one nanosecond past the largest FiniteDuration" in {
      an[IllegalArgumentException] should be thrownBy
        DurationConverters.toScala(JavaDuration.ofNanos(Long.MaxValue).plusNanos(1))
    }
  }
}
