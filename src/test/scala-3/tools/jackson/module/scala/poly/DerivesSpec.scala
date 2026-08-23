package tools.jackson.module.scala.poly

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, SealedSubtypes}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// marked by deriving rather than by extending the marker trait
sealed trait Beast derives SealedSubtypes
case class Wolf(name: String) extends Beast
case object Phantom extends Beast
sealed trait Winged extends Beast
case class Raven(feathers: Int) extends Winged
object Kept { case class Tame(name: String) extends Beast }

case class Lair(beast: Beast)

class DerivesSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def roundTrip[T](value: T, clazz: Class[T]): T = mapper.readValue(mapper.writeValueAsString(value), clazz)

  "SealedSubtypes" should {
    "mark a hierarchy the way the marker trait does" in {
      mapper.writeValueAsString(Lair(Wolf("grey"))) shouldEqual """{"beast":{"@type":"Wolf","name":"grey"}}"""
      mapper.writeValueAsString(Lair(Phantom)) shouldEqual """{"beast":{"@type":"Phantom"}}"""
    }
    "round trip every shape of implementation" in {
      roundTrip(Lair(Wolf("grey")), classOf[Lair]) shouldEqual Lair(Wolf("grey"))
      roundTrip(Lair(Phantom), classOf[Lair]).beast should be theSameInstanceAs Phantom
      roundTrip(Lair(Raven(7)), classOf[Lair]) shouldEqual Lair(Raven(7))
    }
    "reach an implementation through an intermediate sealed trait" in {
      mapper.writeValueAsString(Lair(Raven(7))) shouldEqual """{"beast":{"@type":"Raven","feathers":7}}"""
    }
    "name an implementation declared in another object the same way" in {
      mapper.writeValueAsString(Lair(Kept.Tame("cat"))) shouldEqual """{"beast":{"@type":"Kept.Tame","name":"cat"}}"""
      roundTrip(Lair(Kept.Tame("cat")), classOf[Lair]) shouldEqual Lair(Kept.Tame("cat"))
    }
    "read the base type at the top level" in {
      mapper.readValue("""{"@type":"Wolf","name":"grey"}""", classOf[Beast]) shouldEqual Wolf("grey")
    }
    "deserialize into a collection of the base type" in {
      val json = """[{"@type":"Wolf","name":"grey"},{"@type":"Phantom"}]"""
      mapper.readValue(json, new TypeReference[List[Beast]] {}) shouldEqual List(Wolf("grey"), Phantom)
    }
    "reject a @type that names something outside the hierarchy" in {
      intercept[IllegalArgumentException] {
        mapper.readValue("""{"@type":"Dog","name":"rex"}""", classOf[Beast])
      }
    }
  }
}
