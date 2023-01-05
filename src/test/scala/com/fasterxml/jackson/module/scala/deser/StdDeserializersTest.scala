package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.json.JsonMapper


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

  it should "deserialize to BigDecimal a number in exponent form" in { f =>
    Seq("1.0E+2", "1.0e+2", "1.0e2", "1.234e-234").foreach { numString =>
      f.readValue(numString, classOf[BigDecimal]) shouldBe BigDecimal(numString)
    }
  }

  it should "deserialize to BigDecimal a number in exponent form (Fast number parsing)" in { _ =>
    val mapper = JsonMapper.builder()
      .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
      .build()
    Seq("1.0E+2", "1.0e+2", "1.0e2", "1.234e-234").foreach { numString =>
      mapper.readValue(numString, classOf[BigDecimal]) shouldBe BigDecimal(numString)
    }
  }

  it should "deserialize to BigInt a number in exponent form" in { f =>
    Seq("1.0E2", "1.0e2", "1.0e2", "10000E-2").foreach { numString =>
      f.readValue(numString, classOf[BigInt]) shouldBe BigDecimal(numString).toBigIntExact.orNull
    }
  }

  // https://github.com/FasterXML/jackson-module-scala/issues/616
  it should "deserialize to BigInt a number in exponent form (Fast number parsing)" ignore { _ =>
    val mapper = JsonMapper.builder()
      .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
      .build()
    Seq("1.0E2", "1.0e2", "1.0e2", "10000E-2").foreach { numString =>
      mapper.readValue(numString, classOf[BigInt]) shouldBe BigDecimal(numString).toBigIntExact.orNull
    }
  }
}
