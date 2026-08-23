package tools.jackson.module.scala.poly

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * A polymorphic value holding a polymorphic value, with each of the two hierarchies handled by this
 * module or by Jackson's own annotations.
 */
class NestedPolymorphismSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  private def roundTrip[T](value: T, clazz: Class[T]): T = mapper.readValue(mapper.writeValueAsString(value), clazz)

  "SimplePolymorphismModule" should {
    "tag both levels when both hierarchies are marked" in {
      mapper.writeValueAsString(NestHolder(OuterA(InnerA(1)))) shouldEqual
        """{"outer":{"@type":"OuterA","inner":{"@type":"InnerA","a":1}}}"""
    }
    "tag both levels when the inner value is a case object" in {
      mapper.writeValueAsString(NestHolder(OuterA(InnerB))) shouldEqual
        """{"outer":{"@type":"OuterA","inner":{"@type":"InnerB"}}}"""
    }
    "round trip both hierarchies when both are marked" in {
      roundTrip(NestHolder(OuterA(InnerA(1))), classOf[NestHolder]) shouldEqual NestHolder(OuterA(InnerA(1)))
      roundTrip(NestHolder(OuterA(InnerB)), classOf[NestHolder]).outer match {
        case OuterA(inner) => inner should be theSameInstanceAs InnerB
        case other => fail(s"expected OuterA, got $other")
      }
      roundTrip(NestHolder(OuterB), classOf[NestHolder]).outer should be theSameInstanceAs OuterB
    }
    "tag every element of a collection of the inner hierarchy" in {
      mapper.writeValueAsString(NestHolder(OuterC(Seq(InnerA(1), InnerB)))) shouldEqual
        """{"outer":{"@type":"OuterC","inners":[{"@type":"InnerA","a":1},{"@type":"InnerB"}]}}"""
      val value = NestHolder(OuterC(Seq(InnerA(1), InnerB)))
      roundTrip(value, classOf[NestHolder]) shouldEqual value
    }
    "tag a hierarchy nested inside itself" in {
      val value = NestHolder(OuterNest(OuterNest(OuterA(InnerA(1)))))
      mapper.writeValueAsString(value) shouldEqual
        """{"outer":{"@type":"OuterNest","next":{"@type":"OuterNest","next":{"@type":"OuterA","inner":{"@type":"InnerA","a":1}}}}}"""
      roundTrip(value, classOf[NestHolder]) shouldEqual value
    }
    "let Jackson own the outer hierarchy and keep the inner one" in {
      mapper.writeValueAsString(AnnOuterHolder(AnnOuterA(InnerA(1)))) shouldEqual
        """{"outer":{"kind":"AnnOuterA","inner":{"@type":"InnerA","a":1}}}"""
    }
    "round trip when Jackson owns only the outer hierarchy" in {
      roundTrip(AnnOuterHolder(AnnOuterA(InnerA(1))), classOf[AnnOuterHolder]) shouldEqual
        AnnOuterHolder(AnnOuterA(InnerA(1)))
      roundTrip(AnnOuterHolder(AnnOuterA(InnerB)), classOf[AnnOuterHolder]) shouldEqual
        AnnOuterHolder(AnnOuterA(InnerB))
      roundTrip(AnnOuterHolder(AnnOuterB("x")), classOf[AnnOuterHolder]) shouldEqual AnnOuterHolder(AnnOuterB("x"))
    }
    "let Jackson own the inner hierarchy and keep the outer one" in {
      mapper.writeValueAsString(PlainOuterHolder(PlainOuterA(AnnInnerA(1)))) shouldEqual
        """{"outer":{"@type":"PlainOuterA","inner":{"kind":"AnnInnerA","a":1}}}"""
    }
    "round trip when Jackson owns only the inner hierarchy" in {
      roundTrip(PlainOuterHolder(PlainOuterA(AnnInnerA(1))), classOf[PlainOuterHolder]) shouldEqual
        PlainOuterHolder(PlainOuterA(AnnInnerA(1)))
      roundTrip(PlainOuterHolder(PlainOuterA(AnnInnerB("b"))), classOf[PlainOuterHolder]) shouldEqual
        PlainOuterHolder(PlainOuterA(AnnInnerB("b")))
      roundTrip(PlainOuterHolder(PlainOuterB), classOf[PlainOuterHolder]).outer should be theSameInstanceAs PlainOuterB
    }
  }
}
