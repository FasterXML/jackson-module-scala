package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, SealedPolymorphismSupport}
import tools.jackson.module.scala.poly.mixin.VehicleMixin
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * A hierarchy that cannot be changed opts in through a Jackson mix-in carrying the marker, rather
 * than by extending it. A mix-in does not change the type on the JVM, so the marker is not
 * inherited by the implementations - the module has to read the registration from the mapper's
 * config instead.
 *
 * The mix-in lives in another package, as a third party's would. It cannot extend `Vehicle`: that is
 * "illegal inheritance from sealed trait Vehicle" outside the file the hierarchy is declared in,
 * which is exactly the situation this exists for. Jackson does not require a mix-in to be a subtype
 * of what it is mixed into.
 */
class MixinSpec extends AnyWordSpec with Matchers {

  private def mapperWith(mixin: Class[_]) =
    JsonMapper.builder().addModule(DefaultScalaModule).addMixIn(classOf[Vehicle], mixin).build()

  "SealedPolymorphismModule" should {
    "tag a hierarchy that opts in through a mix-in on its base" in {
      val mapper = mapperWith(classOf[VehicleMixin])
      mapper.writeValueAsString(Garage(Car(4))) shouldEqual """{"vehicle":{"@type":"Car","wheels":4}}"""
      mapper.writeValueAsString(Garage(Bike)) shouldEqual """{"vehicle":{"@type":"Bike"}}"""
    }
    "round trip a hierarchy that opts in through a mix-in" in {
      val mapper = mapperWith(classOf[VehicleMixin])
      def roundTrip(value: Garage) = mapper.readValue(mapper.writeValueAsString(value), classOf[Garage])
      roundTrip(Garage(Car(4))) shouldEqual Garage(Car(4))
      roundTrip(Garage(Truck(3, "sand"))) shouldEqual Garage(Truck(3, "sand"))
      roundTrip(Garage(Bike)).vehicle should be theSameInstanceAs Bike
    }
    "reach an implementation below an intermediate trait" in {
      val mapper = mapperWith(classOf[VehicleMixin])
      mapper.writeValueAsString(Garage(Van(3))) shouldEqual """{"vehicle":{"@type":"Van","seats":3}}"""
      mapper.readValue(mapper.writeValueAsString(Garage(Van(3))), classOf[Garage]) shouldEqual Garage(Van(3))
    }
    "accept an anonymous class as the mix-in" in {
      val anonymous = (new SealedPolymorphismSupport {}).getClass
      val mapper = mapperWith(anonymous)
      mapper.writeValueAsString(Garage(Car(4))) shouldEqual """{"vehicle":{"@type":"Car","wheels":4}}"""
      mapper.readValue(mapper.writeValueAsString(Garage(Car(4))), classOf[Garage]) shouldEqual Garage(Car(4))
    }
    "read the base type at the top level through a mix-in" in {
      val mapper = mapperWith(classOf[VehicleMixin])
      mapper.readValue("""{"@type":"Truck","axles":2,"load":"gravel"}""", classOf[Vehicle]) shouldEqual Truck(2, "gravel")
    }
    "leave the same hierarchy alone on a mapper without the mix-in" in {
      val plain = JsonMapper.builder().addModule(DefaultScalaModule).build()
      plain.writeValueAsString(Garage(Car(4))) shouldEqual """{"vehicle":{"wheels":4}}"""
    }
    "not let a mix-in without the marker opt a hierarchy in" in {
      val mapper = mapperWith(classOf[Garage])
      mapper.writeValueAsString(Garage(Car(4))) shouldEqual """{"vehicle":{"wheels":4}}"""
    }
  }
}
