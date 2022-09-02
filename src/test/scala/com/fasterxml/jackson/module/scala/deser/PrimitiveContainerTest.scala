package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.deser.OptionWithNumberDeserializerTest.{OptionLong, OptionLongWithDefault}

object PrimitiveContainerTest {

  case class OptionInt(value: Option[Int])
  case class AnnotatedOptionInt(@JsonDeserialize(contentAs = classOf[java.lang.Integer]) value: Option[Int])
  case class AnnotatedOptionLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: Option[Long])

  case class AnnotatedHashKeyLong(@JsonDeserialize(keyAs = classOf[java.lang.Long]) value: Map[Long, String])
  case class AnnotatedHashValueLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: Map[String, Long])
}

class PrimitiveContainerTest extends DeserializationFixture
{
  import PrimitiveContainerTest._

  behavior of "Primitive Containers"

  it should "support deserializing primitives (int)" in { f =>
    val value = f.readValue("""{"value":1}""", new TypeReference[OptionInt] {})
    value.value shouldBe Some(1)
  }

  it should "support deserializing primitives (long)" in { f =>
    val value = f.readValue("""{"valueLong":1}""", new TypeReference[OptionLong] {})
    value.valueLong shouldBe Some(1L)
    val max = Long.MaxValue
    val value2 = f.readValue(s"""{"valueLong":$max}""", new TypeReference[OptionLong] {})
    value2.valueLong shouldBe Some(max)
    val value3 = f.readValue("{}", new TypeReference[OptionLong] {})
    value3.valueLong shouldBe empty
  }

  it should "support deserializing OptionLongWithDefault" in { f =>
    val value = f.readValue("""{"valueLong":1}""", new TypeReference[OptionLongWithDefault] {})
    value.valueLong shouldBe Some(1L)
    val max = Long.MaxValue
    val value2 = f.readValue(s"""{"valueLong":$max}""", new TypeReference[OptionLongWithDefault] {})
    value2.valueLong shouldBe Some(max)
    val value3 = f.readValue("{}", new TypeReference[OptionLongWithDefault] {})
    value3.valueLong shouldBe empty
  }

  it should "support primitive conversions in" in { f =>
    val value = f.readValue("""{"value":"1"}""", new TypeReference[AnnotatedOptionInt] {})
    value.value shouldBe Some(1)
  }

  it should "support type widening"  in { f =>
    val value = f.readValue("""{"value":1}""", new TypeReference[AnnotatedOptionLong] {})
    value.value.get shouldBe 1L
  }

  it should "enforce type constraints"  in { f =>
    val thrown = intercept[JsonMappingException] {
      f.readValue("""{"value":9223372036854775807}""", new TypeReference[AnnotatedOptionInt] {}).value.get
    }
    thrown.getMessage should startWith ("Numeric value (9223372036854775807) out of range")
  }

  it should "support map keys" in { f =>
    val value = f.readValue("""{"value":{"1":"one"}}""", new TypeReference[AnnotatedHashKeyLong] {})
    value.value should contain key 1L
    value.value(1L) shouldBe "one"
  }

  it should "support map values" in { f =>
    val value = f.readValue("""{"value":{"key": "1"}}""", new TypeReference[AnnotatedHashValueLong] {})
    value.value should contain key "key"
    value.value("key") shouldBe 1L
  }
}
