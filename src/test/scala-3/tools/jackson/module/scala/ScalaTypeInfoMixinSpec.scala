package tools.jackson.module.scala

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonCreator.Mode
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.json.JsonMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Try

// a class from elsewhere, which cannot be given a derives clause
case class Foreign(aLong: Option[Long], pairs: Seq[(String, Long)], name: Option[String])

// describes it by extending it: the derives sees Foreign's own members, so none is repeated
trait ForeignMixin extends Foreign derives ScalaTypeInfo

// or by repeating the members it describes - not necessarily all of them - which is the form left
// for a final class, that no trait can extend
case class RepeatingMixin(aLong: Option[Long], pairs: Seq[(String, Long)]) derives ScalaTypeInfo
case class PartialMixin(aLong: Option[Long]) derives ScalaTypeInfo

final case class Sealed(aLong: Option[Long])
case class SealedMixin(aLong: Option[Long]) derives ScalaTypeInfo

// the annotation form, which any Scala version can use
trait ForeignAnnotated {
  @JsonDeserialize(contentAs = classOf[Long]) def aLong: Option[Long]
}

// a class with its own derives, and a mix-in that says otherwise
case class Own(aLong: Option[Long]) derives ScalaTypeInfo
case class OwnMixin(aLong: Option[Int]) derives ScalaTypeInfo
trait OwnAnnotated {
  @JsonDeserialize(contentAs = classOf[Int]) def aLong: Option[Long]
}

// read through a companion creator: the mix-in's companion carries the same creator
case class ForeignMade private (count: Option[Long])
object ForeignMade {
  @JsonCreator(mode = Mode.PROPERTIES)
  def make(count: Option[Long]): ForeignMade = new ForeignMade(count)
}
case class ForeignMadeMixin(count: Option[Long]) derives ScalaTypeInfo
object ForeignMadeMixin {
  @JsonCreator(mode = Mode.PROPERTIES)
  def make(count: Option[Long]): ForeignMadeMixin = new ForeignMadeMixin(count)
}

/**
 * A class that cannot be changed is described by a Jackson mix-in, as a sealed hierarchy that cannot
 * be changed opts in through one: a mix-in with its own `derives ScalaTypeInfo`, or one carrying the
 * `@JsonDeserialize` a field could have carried.
 */
class ScalaTypeInfoMixinSpec extends AnyWordSpec with Matchers {

  private val plain = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def mapperWith(target: Class[?], mixin: Class[?]) =
    JsonMapper.builder().addModule(DefaultScalaModule).addMixIn(target, mixin).build()

  private val json = """{"aLong":2,"pairs":[["a",3]],"name":"x"}"""

  // what an Option[Long] field actually holds, which a small number may have been read as
  private def held(value: Any, field: String): Option[Class[?]] =
    value.getClass.getMethod(field).invoke(value).asInstanceOf[Option[Any]].map(_.getClass)

  "ScalaTypeInfo" should {
    "describe a class through a mix-in that derives it" in {
      val read = mapperWith(classOf[Foreign], classOf[ForeignMixin]).readValue(json, classOf[Foreign])
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      read.pairs.map(_._2 + 1L) shouldEqual Seq(4L)
      read.name shouldEqual Some("x")
    }
    "describe a class through a mix-in that repeats its members" in {
      val read = mapperWith(classOf[Foreign], classOf[RepeatingMixin]).readValue(json, classOf[Foreign])
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      read.pairs.map(_._2 + 1L) shouldEqual Seq(4L)
    }
    "describe a final class through a mix-in that repeats its members" in {
      val read = mapperWith(classOf[Sealed], classOf[SealedMixin]).readValue("""{"aLong":2}""", classOf[Sealed])
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
    }
    "describe only the members the mix-in has" in {
      val read = mapperWith(classOf[Foreign], classOf[PartialMixin]).readValue(json, classOf[Foreign])
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
      Try(read.pairs.map(_._2 + 1L)).toEither.left.map(_.getClass) shouldEqual Left(classOf[ClassCastException])
    }
    "leave the class alone on a mapper without the mix-in" in {
      val read = plain.readValue(json, classOf[Foreign])
      held(read, "aLong") shouldEqual Some(classOf[java.lang.Integer])
    }
    "describe a class through a mix-in carrying the annotation" in {
      val read = mapperWith(classOf[Foreign], classOf[ForeignAnnotated]).readValue(json, classOf[Foreign])
      read.aLong.map(_ + 1L) shouldEqual Some(3L)
    }
    "prefer what a mix-in derived to what the class derived" in {
      plain.readValue("""{"aLong":2}""", classOf[Own]).aLong.map(_ + 1L) shouldEqual Some(3L)
      val read = mapperWith(classOf[Own], classOf[OwnMixin]).readValue("""{"aLong":2}""", classOf[Own])
      held(read, "aLong") shouldEqual Some(classOf[java.lang.Integer])
    }
    "prefer an annotation a mix-in carries to what the class derived" in {
      val read = mapperWith(classOf[Own], classOf[OwnAnnotated]).readValue("""{"aLong":2}""", classOf[Own])
      held(read, "aLong") shouldEqual Some(classOf[java.lang.Integer])
    }
    "describe the parameters of a companion creator through a mix-in" in {
      held(plain.readValue("""{"count":2}""", classOf[ForeignMade]), "count") shouldEqual Some(classOf[java.lang.Integer])
      val read = mapperWith(classOf[ForeignMade], classOf[ForeignMadeMixin]).readValue("""{"count":2}""", classOf[ForeignMade])
      read.count.map(_ + 1L) shouldEqual Some(3L)
    }
  }
}
