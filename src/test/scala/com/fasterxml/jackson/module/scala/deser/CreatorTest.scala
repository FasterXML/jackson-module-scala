package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.IntNode
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

  sealed abstract class Struct1(val name: String, val classifier: String = "my_default_string") {
    override def toString: String = name
  }

  case class ConstructorWithDefaultValues(s: String = "some string", i: Int = 10, dummy: String)

  case class ConstructorWithOptionDefaultValues(s: Option[String] = None, i: Option[Int] = None, dummy: String)

  case class ConstructorWithOptionSeqDefaultValues(s: Option[Seq[String]] = None)

  case class ConstructorWithOptionStruct(s: Option[Struct1] = None)

  class PositiveLong private (val value: Long) {
    override def toString() = s"PositiveLong($value)"
  }
  object PositiveLong {
    @JsonCreator
    def apply(long: Long): PositiveLong = new PositiveLong(long)
    @JsonCreator
    def apply(str: String): PositiveLong = new PositiveLong(str.toLong)
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

  it should "support default values" in { f =>
    val deser = f.readValue[ConstructorWithDefaultValues]("""{}""")
    deser.s shouldEqual "some string"
    deser.i shouldEqual 10
    deser.dummy shouldEqual null
    val deser2 = f.readValue[ConstructorWithDefaultValues]("""{"s":"passed","i":5}""")
    deser2.s shouldEqual "passed"
    deser2.i shouldEqual 5
  }

  it should "support options with default values" in { f =>
    val deser = f.readValue[ConstructorWithOptionDefaultValues]("""{}""")
    deser.s shouldBe empty
    deser.i shouldBe empty
    deser.dummy shouldEqual null
    val deser2 = f.readValue[ConstructorWithOptionDefaultValues]("""{"s":"passed","i":5}""")
    deser2.s shouldEqual Some("passed")
    deser2.i shouldEqual Some(5)
    f.writeValueAsString(ConstructorWithOptionDefaultValues(dummy="d")) shouldEqual """{"s":null,"i":null,"dummy":"d"}"""
  }

  it should "support optional seqs with default values" in { f =>
    val deser = f.readValue[ConstructorWithOptionSeqDefaultValues]("""{}""")
    deser.s shouldBe empty
    val deser2 = f.readValue[ConstructorWithOptionSeqDefaultValues]("""{"s":["a", "b"]}""")
    deser2.s shouldEqual Some(Seq("a", "b"))
    f.writeValueAsString(ConstructorWithOptionSeqDefaultValues()) shouldEqual """{"s":null}"""
  }

  it should "support optional structs with default values" ignore { f =>
    val deser = f.readValue[ConstructorWithOptionStruct]("""{}""")
    deser.s shouldBe empty
    val deser2 = f.readValue[ConstructorWithOptionStruct]("""{"s":{"name":"name"}}""")
    deser2.s shouldEqual Some(new Struct1("name"){})
    f.writeValueAsString(ConstructorWithOptionStruct()) shouldEqual """{"s":null}"""
  }

  it should "support multiple creator annotations" in { f =>
    val node: JsonNode = f.valueToTree[IntNode](10)
    f.convertValue(node, new TypeReference[PositiveLong] {}) shouldEqual PositiveLong(node.asLong())
  }
}
