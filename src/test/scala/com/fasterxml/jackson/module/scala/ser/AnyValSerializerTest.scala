package __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JsonValue
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.BaseFixture

import scala.annotation.meta.getter

object AnyValSerializerTest {

  case class IntAnyVal(@(JsonValue @getter) underlying: Int) extends AnyVal
  case class IntAnyValHolder(value: IntAnyVal)

  case class LI(values: List[IntAnyVal])
  case class IntIter(values: Iterator[IntAnyVal])

  case class DoubleAnyVal(underlying: Double) extends AnyVal
  case class DoubleAnyValHolder(value: DoubleAnyVal)

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
    mapper.writeValueAsString(value) shouldBe """42"""
    mapper.writeValueAsString(IntAnyValHolder(value)) shouldBe """{"value":42}"""
  }

  it should "serialize a list of Int AnyVal" in { mapper =>
    mapper.writeValueAsString(LI(List(IntAnyVal(42), IntAnyVal(99)))) shouldBe """{"values":[42,99]}"""
  }

  it should "serialize a Iterator of Int AnyVal" in { mapper =>
    val list = List(IntAnyVal(42), IntAnyVal(99))
    mapper.writeValueAsString(IntIter(list.iterator)) shouldBe """{"values":[42,99]}"""
  }

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

  it should "serialize an BigDecimal AnyVal" in { mapper =>
    val value = BD(42)
    mapper.writeValueAsString(value) shouldBe "42"
    mapper.writeValueAsString(BDH(value)) shouldBe """{"value":42}"""
  }

  it should "serialize a List of BigDecimal AnyVal" in { mapper =>
    mapper.writeValueAsString(LBD(List(BD(42), BD(99)))) shouldBe """{"values":[42,99]}"""
  }
}
