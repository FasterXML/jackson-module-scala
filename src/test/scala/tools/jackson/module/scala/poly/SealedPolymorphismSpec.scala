package tools.jackson.module.scala.poly

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{DefaultScalaModule, ScalaModule, SealedPolymorphismModule}
import tools.jackson.module.scala.deser.SealedPolymorphismDeserializerModule
import tools.jackson.module.scala.ser.SealedPolymorphismSerializerModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SealedPolymorphismSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def roundTrip[T](value: T, clazz: Class[T]): T = mapper.readValue(mapper.writeValueAsString(value), clazz)

  private def rootCause(error: Throwable): Throwable =
    Option(error.getCause).filter(_ ne error).map(rootCause).getOrElse(error)

  "SealedPolymorphismModule" should {
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
    "qualify implementations declared in objects that do not enclose the base" in {
      mapper.writeValueAsString(DupHolder(FirstGroup.Same(1))) shouldEqual """{"d":{"@type":"FirstGroup$Same","v":1}}"""
      mapper.writeValueAsString(DupHolder(SecondGroup.Same("x"))) shouldEqual """{"d":{"@type":"SecondGroup$Same","v":"x"}}"""
      mapper.writeValueAsString(DupHolder(FirstGroup.Only)) shouldEqual """{"d":{"@type":"FirstGroup$Only"}}"""
    }
    "round trip same named implementations from different objects" in {
      roundTrip(DupHolder(FirstGroup.Same(1)), classOf[DupHolder]) shouldEqual DupHolder(FirstGroup.Same(1))
      roundTrip(DupHolder(SecondGroup.Same("x")), classOf[DupHolder]) shouldEqual DupHolder(SecondGroup.Same("x"))
      roundTrip(DupHolder(FirstGroup.Only), classOf[DupHolder]).d should be theSameInstanceAs FirstGroup.Only
    }
    // serializing only needs the value's own class, so an implementation resolution cannot reach
    // would otherwise be written happily and fail only when something read it back. What each Scala
    // version can work out about the hierarchy differs, so only the refusal is asserted here - see
    // Scala2LookupSpec and Scala3LookupSpec for what each can say about it.
    "refuse to write an implementation of a hierarchy that is not sealed" in {
      val message = String.valueOf(rootCause(intercept[Exception] {
        mapper.writeValueAsString(UnsealedHolder(other.Faraway(1)))
      }).getMessage)
      message should include("sealed")
    }
    "refuse to write an implementation whose derived name is already taken" in {
      val message = String.valueOf(rootCause(intercept[Exception] {
        mapper.writeValueAsString(ShadowHolder(Entry("x")))
      }).getMessage)
      message should include("already belongs to")
    }
    // `sealed class Node` is instantiable, so the root carries a name of its own and a property
    // declared at that root still has to dispatch
    "tag a concrete root as well as its subclasses" in {
      mapper.writeValueAsString(Tree(new Node(1))) shouldEqual """{"node":{"@type":"Node","id":1}}"""
      mapper.writeValueAsString(Tree(new Branch(2, "b"))) shouldEqual """{"node":{"@type":"Branch","id":2,"label":"b"}}"""
    }
    "read a concrete root back as itself" in {
      val node = mapper.readValue("""{"node":{"@type":"Node","id":1}}""", classOf[Tree]).node
      node.getClass shouldEqual classOf[Node]
      node.id shouldEqual 1
    }
    "read a subclass of a concrete root without losing it" in {
      val node = mapper.readValue(mapper.writeValueAsString(Tree(new Branch(2, "b"))), classOf[Tree]).node
      node shouldBe a[Branch]
      node.id shouldEqual 2
      node.asInstanceOf[Branch].label shouldEqual "b"
    }
    "dispatch at a concrete type part way down the hierarchy" in {
      val json = mapper.writeValueAsString(Limb(new Twig(3, "t", 9)))
      json shouldEqual """{"branch":{"@type":"Twig","id":3,"label":"t","length":9}}"""
      val branch = mapper.readValue(json, classOf[Limb]).branch
      branch shouldBe a[Twig]
      branch.asInstanceOf[Twig].length shouldEqual 9
      branch.label shouldEqual "t"
    }
    "read an untagged object at a concrete declared type" in {
      val branch = mapper.readValue("""{"branch":{"id":4,"label":"u"}}""", classOf[Limb]).branch
      branch.getClass shouldEqual classOf[Branch]
      branch.label shouldEqual "u"
    }
    "give each module instance state of its own" in {
      val first = new SealedPolymorphismModule {}
      val second = new SealedPolymorphismModule {}
      first.sealedPolymorphism should not be theSameInstanceAs(second.sealedPolymorphism)
      SealedPolymorphismModule.sealedPolymorphism should not be theSameInstanceAs(first.sealedPolymorphism)
    }
    "share one instance between the two halves of a module" in {
      val module = new SealedPolymorphismModule {}
      (module: SealedPolymorphismSerializerModule).sealedPolymorphism should be theSameInstanceAs
        (module: SealedPolymorphismDeserializerModule).sealedPolymorphism
    }
    "tie the state of a built ScalaModule to that build" in {
      val builder = ScalaModule.builder().addAllBuiltinModules()
      builder.sealedPolymorphismModule.sealedPolymorphism should not be
        theSameInstanceAs(SealedPolymorphismModule.sealedPolymorphism)
      val built = JsonMapper.builder().addModule(builder.build()).build()
      val value = Owner("ann", Dog("rex"))
      built.readValue(built.writeValueAsString(value), classOf[Owner]) shouldEqual value
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
