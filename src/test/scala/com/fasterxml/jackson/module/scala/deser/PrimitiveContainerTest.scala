package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.core.JsonParseException

object PrimitiveContainerTest
{
  case class OptionInt(value: Option[Int])
  case class AnnotatedOptionInt(@JsonDeserialize(contentAs = classOf[java.lang.Integer]) value: Option[Int])
  case class OptionLong(value: Option[Long])
  case class AnnotatedOptionLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: Option[Long])

  case class AnnotatedHashKeyLong(@JsonDeserialize(keyAs = classOf[java.lang.Long]) value: Map[Long,String])
  case class AnnotatedHashValueLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: Map[String,Long])
}

@RunWith(classOf[JUnitRunner])
class PrimitiveContainerTest extends DeserializationFixture with ShouldMatchers
{
  import PrimitiveContainerTest._

  behavior of "Primitive Containers"

  it should "support deserializing primitives" in { f =>
    val value = f.readValue[OptionInt]("""{"value":1}""")
    value.value should be === Some(1)
  }

  it should "support primitive conversions in" in { f =>
    val value = f.readValue[AnnotatedOptionInt]("""{"value":"1"}""")
    value.value should be === Some(1)
  }

  it should "support type widening"  in { f =>
    val value = f.readValue[AnnotatedOptionLong]("""{"value":1}""")
    value.value.get should be === 1L
  }

  it should "enforce type constraints"  in { f =>
    val thrown = intercept[JsonParseException] {
      f.readValue[AnnotatedOptionInt]("""{"value":9223372036854775807}""").value.get
    }
    thrown.getMessage should startWith ("Numeric value (9223372036854775807) out of range")
  }

  it should "support map keys" ignore { f =>
    val value = f.readValue[AnnotatedHashKeyLong]("""{"value":{"1":"one"}}""")
    value.value should contain key 1L
    value.value(1L) should be === "one"
  }

  it should "support map values" in { f =>
    val value = f.readValue[AnnotatedHashValueLong]("""{"value":{"key": "1"}}""")
    value.value should contain key "key"
    value.value("key") should be === 1L
  }

}
