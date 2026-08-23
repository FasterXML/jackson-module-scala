package tools.jackson.module.scala.poly

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SimplePolymorphismSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def roundTrip[T](value: T, clazz: Class[T]): T = mapper.readValue(mapper.writeValueAsString(value), clazz)

  private def rootCause(error: Throwable): Throwable =
    Option(error.getCause).filter(_ ne error).map(rootCause).getOrElse(error)

  "SimplePolymorphismModule" should {
    "tag an implementation declared beside the base type" in {
      mapper.writeValueAsString(Owner("ann", Dog("rex"))) shouldEqual """{"name":"ann","pet":{"@type":"Dog","name":"rex"}}"""
    }
    "tag a case object" in {
      mapper.writeValueAsString(Owner("ann", Unknown)) shouldEqual """{"name":"ann","pet":{"@type":"Unknown"}}"""
    }
    "round trip an implementation declared beside the base type" in {
      roundTrip(Owner("ann", Dog("rex")), classOf[Owner]) shouldEqual Owner("ann", Dog("rex"))
      roundTrip(Owner("ann", Bird("tweety", true)), classOf[Owner]) shouldEqual Owner("ann", Bird("tweety", true))
    }
    "round trip a case object to the same instance" in {
      roundTrip(Owner("ann", Unknown), classOf[Owner]).pet should be theSameInstanceAs Unknown
    }
    "round trip a sealed abstract class base" in {
      roundTrip(Drawing(Rect(2.0, 3.0)), classOf[Drawing]) shouldEqual Drawing(Rect(2.0, 3.0))
      roundTrip(Drawing(Point), classOf[Drawing]).shape should be theSameInstanceAs Point
    }
    "round trip implementations declared in the companion of the base type" in {
      mapper.writeValueAsString(Order(10, Payment.Card("4242"))) shouldEqual """{"total":10,"payment":{"@type":"Card","last4":"4242"}}"""
      roundTrip(Order(10, Payment.Card("4242")), classOf[Order]) shouldEqual Order(10, Payment.Card("4242"))
      roundTrip(Order(10, Payment.Cash), classOf[Order]).payment should be theSameInstanceAs Payment.Cash
    }
    "round trip a hierarchy nested in an unrelated object" in {
      mapper.writeValueAsString(Envelope(Wrapper.Created(7))) shouldEqual """{"event":{"@type":"Created","id":7}}"""
      roundTrip(Envelope(Wrapper.Created(7)), classOf[Envelope]) shouldEqual Envelope(Wrapper.Created(7))
      roundTrip(Envelope(Wrapper.Deleted), classOf[Envelope]).event should be theSameInstanceAs Wrapper.Deleted
    }
    "round trip a collection of implementations" in {
      val shelter = Shelter(Seq(Dog("rex"), Unknown, Bird("tweety", false)))
      roundTrip(shelter, classOf[Shelter]) shouldEqual shelter
    }
    "read the base type at the top level" in {
      mapper.readValue("""{"@type":"Dog","name":"rex"}""", classOf[Animal]) shouldEqual Dog("rex")
    }
    "read a value declared as the implementation type, tag and all" in {
      mapper.readValue("""{"@type":"Dog","name":"rex"}""", classOf[Dog]) shouldEqual Dog("rex")
    }
    "round trip an Option of the base type" in {
      roundTrip(MaybeOwner("ann", Some(Dog("rex"))), classOf[MaybeOwner]) shouldEqual MaybeOwner("ann", Some(Dog("rex")))
      roundTrip(MaybeOwner("ann", None), classOf[MaybeOwner]) shouldEqual MaybeOwner("ann", None)
    }
    "round trip a Map valued by the base type" in {
      val zoo = Zoo(Map("a" -> Dog("rex"), "b" -> Unknown))
      roundTrip(zoo, classOf[Zoo]) shouldEqual zoo
    }
    "defer to JsonTypeInfo when the hierarchy is also annotated" in {
      mapper.writeValueAsString(Ledger(Cheque(7))) shouldEqual """{"entry":{"kind":"Cheque","number":7}}"""
      roundTrip(Ledger(Cheque(7)), classOf[Ledger]) shouldEqual Ledger(Cheque(7))
    }
    "let the enum support own a marked Scala 3 enum" in {
      // the enum rules apply throughout: a simple case is its plain name, a parameterized one an object
      mapper.writeValueAsString(Job(Status.Active)) shouldEqual """{"status":"Active"}"""
      mapper.writeValueAsString(Job(Status.Failed(1))) shouldEqual """{"status":{"@type":"Failed","code":1}}"""
      roundTrip(Job(Status.Failed(1)), classOf[Job]) shouldEqual Job(Status.Failed(1))
      roundTrip(Job(Status.Active), classOf[Job]).status should be theSameInstanceAs Status.Active
    }
    // Jackson makes the annotated class a polymorphic base in its own right, so reading it would
    // demand that annotation's type id - which nothing in a marked hierarchy ever writes
    "report JsonTypeInfo on an implementation rather than the base" in {
      val error = intercept[Exception] {
        mapper.writeValueAsString(LeafHolder(LeafA(1)))
      }
      val message = String.valueOf(rootCause(error).getMessage)
      message should include("LeafA")
      message should include("carries @JsonTypeInfo")
      message should include("rooted at")
    }
    "leave other implementations of that hierarchy working" in {
      roundTrip(LeafHolder(LeafB(2)), classOf[LeafHolder]) shouldEqual LeafHolder(LeafB(2))
    }
    "leave an unmarked hierarchy alone" in {
      mapper.writeValueAsString(PlainDog("rex")) shouldEqual """{"name":"rex"}"""
    }
    "reject a @type that names something outside the hierarchy" in {
      intercept[IllegalArgumentException] {
        mapper.readValue("""{"@type":"Rect","width":1.0,"height":2.0}""", classOf[Animal])
      }
    }
    "reject a @type that tries to name a fully qualified class" in {
      intercept[IllegalArgumentException] {
        mapper.readValue("""{"@type":"tools.jackson.module.scala.poly.Dog","name":"rex"}""", classOf[Animal])
      }
    }
    "reject a missing @type" in {
      intercept[IllegalArgumentException] {
        mapper.readValue("""{"name":"rex"}""", classOf[Animal])
      }
    }
    "deserialize into a generic collection of the base type" in {
      val json = """[{"@type":"Dog","name":"rex"},{"@type":"Unknown"}]"""
      val animals = mapper.readValue(json, new TypeReference[List[Animal]] {})
      animals shouldEqual List(Dog("rex"), Unknown)
    }
  }
}
