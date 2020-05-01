package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JsonCreator
import __foursquare_shaded__.com.fasterxml.jackson.databind.ObjectMapper
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

object CreatorTest
{
  class CreatorTestBean(val a: String, var b: String)
  case class CreatorTestCase(a: String, b: String)

  sealed abstract class AbstractBase(val timestamp: Long)
  case class DerivedCase(override val timestamp: Long, name: String) extends AbstractBase(timestamp)

  class CreatorModeBean @JsonCreator(mode=JsonCreator.Mode.DELEGATING)(val s : String)
  case class CreatorModeWrapper (a: CreatorModeBean)

  class AlternativeConstructor(val script: String, dummy: Int) {
    @JsonCreator
    def this(script: String) = {
      this(script, 0)
    }
    override def equals(o: Any): Boolean = o match {
      case ac: AlternativeConstructor => script == ac.script
      case _ => false
    }
  }

  case class MultipleConstructors(script: String, dummy: Int) {
    def this(script: String) = {
      this(script, 0)
    }
  }

  case class MultipleConstructorsAnn @JsonCreator()(script: String, dummy: Int) {
    def this(script: String) = {
      this(script, 0)
    }
  }
}

@RunWith(classOf[JUnitRunner])
class CreatorTest extends DeserializationFixture {
  import CreatorTest._

  behavior of "Creators"

  it should "support constructing regular bean classes" in { f =>
    val bean = f.readValue[CreatorTestBean]("""{"a":"abc","b":"def"}""")
    bean.a shouldBe "abc"
    bean.b shouldBe "def"
  }

  it should "support constructing case classes" in { f =>
    val bean = f.readValue[CreatorTestCase]("""{"a":"abc","b":"def"}""")
    bean shouldBe CreatorTestCase("abc", "def")
  }

  it should "support case classes that override base class properties" in { f =>
    val bean = f.readValue[DerivedCase]("""{"timestamp":1396564798,"name":"foo"}""")
    bean shouldBe DerivedCase(1396564798, "foo")
  }

  it should "honor the JsonCreator mode" in { f =>
    val bean = f.readValue[CreatorModeWrapper]("""{"a":"foo"}""")
    bean.a.s shouldEqual "foo"
  }

  it should "work with static method creator" in { f =>
    val json = "\"2\""

    val regularObjectMapper = new ObjectMapper()

    // Using regular objectMapper
    val v1 = regularObjectMapper.readValue(json, classOf[ValueHolder])
    v1.internalValue shouldEqual 2L

    // Using objectMapper with DefaultScalaModule
    val v2 = f.readValue[ValueHolder](json)
    v2.internalValue shouldEqual 2L
  }

  it should "use secondary constructor annotated with JsonCreator" in { f =>
    val orig = new AlternativeConstructor("abc", 42)
    val bean = f.writeValueAsString(orig)
    bean shouldBe """{"script":"abc"}"""
    val roundTrip = f.readValue[AlternativeConstructor](bean)
    roundTrip shouldEqual orig
  }

  it should "use primary constructor if no JsonCreator annotation" in { f =>
    val orig = MultipleConstructors("abc", 42)
    val bean = f.writeValueAsString(orig)
    bean shouldBe """{"script":"abc","dummy":42}"""
    val roundTrip = f.readValue[MultipleConstructors](bean)
    roundTrip shouldEqual orig
  }

  it should "use primary constructor if primary is JsonCreator annotated" in { f =>
    val orig = MultipleConstructorsAnn("abc", 42)
    val bean = f.writeValueAsString(orig)
    bean shouldBe """{"script":"abc","dummy":42}"""
    val roundTrip = f.readValue[MultipleConstructorsAnn](bean)
    roundTrip shouldEqual orig
  }
}
