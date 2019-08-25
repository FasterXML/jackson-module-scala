package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StdDeserializersTest extends DeserializationFixture {

  behavior of "StdDeserializers"

  it should "deserialize an integer into a scala BigDecimal" in { f =>
    f.readValue[BigDecimal]("1") shouldBe BigDecimal(1)
  }

  it should "deserialize an float into a scala BigDecimal" in { f =>
    f.readValue[BigDecimal]("1.0") shouldBe BigDecimal(1.0)
  }

  it should "deserialize a string into a scala BigDecimal" in { f =>
    f.readValue[BigDecimal]("\"1.0\"") shouldBe BigDecimal("1.0")
  }

  it should "deserialize a float into a scala BigDecimal without losing precision" in { f =>
    val manyDigits = "1.23456789012345678901234567890123456789"

    f.readValue[BigDecimal](manyDigits) shouldBe BigDecimal(manyDigits)
  }
}
