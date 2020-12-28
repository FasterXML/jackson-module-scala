package com.fasterxml.jackson.module.scala.deser


class StdDeserializersTest extends DeserializationFixture {

  behavior of "StdDeserializers"

  it should "deserialize an integer into a scala BigDecimal" in { f =>
    f.readValue("1", classOf[BigDecimal]) shouldBe BigDecimal(1)
  }

  it should "deserialize an float into a scala BigDecimal" in { f =>
    f.readValue("1.0", classOf[BigDecimal]) shouldBe BigDecimal(1.0)
  }

  it should "deserialize a string into a scala BigDecimal" in { f =>
    f.readValue("\"1.0\"", classOf[BigDecimal]) shouldBe BigDecimal("1.0")
  }

  it should "deserialize a float into a scala BigDecimal without losing precision" in { f =>
    val manyDigits = "1.23456789012345678901234567890123456789"

    f.readValue(manyDigits, classOf[BigDecimal]) shouldBe BigDecimal(manyDigits)
  }
}
