package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.BaseFixture

object AnyValSerializerTest {
  case class DoubleAnyVal(underlying: Double) extends AnyVal
  case class DoubleAnyValHolder(value: DoubleAnyVal)

  case class BigIntAnyVal(underlying: BigInt) extends AnyVal
  case class BigIntAnyValHolder(value: BigIntAnyVal)
}

//see AnyValScala2SerializerTest for cases that only work with Scala2
class AnyValSerializerTest extends BaseFixture {
  import AnyValSerializerTest._

  behavior of "AnyVal"

  it should "serialize an Double AnyVal" in { mapper =>
    val value = DoubleAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42.0}"""
    mapper.writeValueAsString(DoubleAnyValHolder(value)) shouldBe """{"value":42.0}"""
  }

  it should "serialize an BigInt AnyVal" in { mapper =>
    val value = BigIntAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42}"""
    mapper.writeValueAsString(BigIntAnyValHolder(value)) shouldBe """{"value":42}"""
  }

}
