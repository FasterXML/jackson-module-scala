package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.scala.BaseFixture

import scala.annotation.meta.getter

object AnyValScala2SerializerTest {

  case class IntAnyVal(@(JsonValue @getter) underlying: Int) extends AnyVal
  case class IntAnyValHolder(value: IntAnyVal)

  case class LI(values: List[IntAnyVal])
  case class IntIter(values: Iterator[IntAnyVal])

  case class BD(@(JsonValue @getter) underlying: BigDecimal) extends AnyVal
  case class BDH(value: BD)
  case class LBD(values: List[BD])
}

//see AnyValSerializerTest for cases that also work with Scala3
class AnyValScala2SerializerTest extends BaseFixture {
  import AnyValScala2SerializerTest._

  behavior of "AnyVal"

  it should "serialize an Int AnyVal" in { mapper =>
    val value = IntAnyVal(42)
    mapper.writeValueAsString(value) shouldBe "42"
    mapper.writeValueAsString(IntAnyValHolder(value)) shouldBe """{"value":42}"""
  }

  it should "serialize a list of Int AnyVal" in { mapper =>
    mapper.writeValueAsString(LI(List(IntAnyVal(42), IntAnyVal(99)))) shouldBe """{"values":[42,99]}"""
  }

  it should "serialize a Iterator of Int AnyVal" in { mapper =>
    val list = List(IntAnyVal(42), IntAnyVal(99))
    mapper.writeValueAsString(IntIter(list.iterator)) shouldBe """{"values":[42,99]}"""
  }

  it should "serialize a BigDecimal AnyVal" in { mapper =>
    val value = BD(42)
    mapper.writeValueAsString(value) shouldBe "42"
    mapper.writeValueAsString(BDH(value)) shouldBe """{"value":42}"""
  }

  it should "serialize a List of BigDecimal AnyVal" in { mapper =>
    mapper.writeValueAsString(LBD(List(BD(42), BD(99)))) shouldBe """{"values":[42,99]}"""
  }
}
