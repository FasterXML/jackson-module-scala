package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.scala.BaseFixture

import scala.annotation.meta.getter

object AnyValSerializerTest {

  case class IntAnyVal(underlying: Int) extends AnyVal
  case class IntAnyValHolder(value: IntAnyVal)

  case class BigIntAnyVal(underlying: BigInt) extends AnyVal
  case class BigIntAnyValHolder(value: BigIntAnyVal)

  case class BD(@(JsonValue @getter) underlying: BigDecimal) extends AnyVal
  case class BDH(value: BD)
  case class LBD(values: List[BD])

}

class AnyValSerializerTest extends BaseFixture {
  import AnyValSerializerTest._

  behavior of "AnyVal"

  it should "serialize an Int AnyVal" in { mapper =>

    val value = IntAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42}"""
    mapper.writeValueAsString(IntAnyValHolder(value)) shouldBe """{"value":42}"""

  }

  it should "serialize an BigInt AnyVal" in { mapper =>

    val value = new BigIntAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42}"""
    mapper.writeValueAsString(new BigIntAnyValHolder(value)) shouldBe """{"value":42}"""

  }

  it should "serialize an BigDecimal AnyVal" in { mapper =>

    val value = new BD(42)
    mapper.writeValueAsString(value) shouldBe "42"
    mapper.writeValueAsString(new BDH(value)) shouldBe """{"value":42}"""

  }

  it should "serialize a List of BigDecimal AnyVal" in { mapper =>

    mapper.writeValueAsString(LBD(List(BD(42), BD(99)))) shouldBe """{"values":[42,99]}"""

  }

}
