package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule


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
    val mapper = newMapperWithFastNumberParsing()
    Seq("1.0E+2", "1.0e+2", "1.0e2", "1.234e-234").foreach { numString =>
      mapper.readValue(numString, classOf[BigDecimal]) shouldBe BigDecimal(numString)
    }
  }

  it should "deserialize to java.math.BigDecimal a number in exponent form (Fast number parsing)" in { _ =>
    val mapper = newMapperWithFastNumberParsing()
    Seq("1.0E+2", "1.0e+2", "1.0e2", "1.234e-234").foreach { numString =>
      mapper.readValue(numString, classOf[java.math.BigDecimal]) shouldBe new java.math.BigDecimal(numString)
    }
  }

  it should "deserialize to BigInt a number in exponent form" in { f =>
    Seq("1.0E2", "1.0e2", "1.0e2", "10000E-2").foreach { numString =>
      f.readValue(numString, classOf[BigInt]) shouldBe BigDecimal(numString).toBigIntExact.orNull
    }
  }

  it should "deserialize to BigInt a number in exponent form (Fast number parsing)" in { _ =>
    val mapper = newMapperWithFastNumberParsing()
    Seq("1.0E2", "1.0e2", "1.0e2", "10000E-2").foreach { numString =>
      mapper.readValue(numString, classOf[BigInt]) shouldBe BigDecimal(numString).toBigIntExact.orNull
    }
  }

  private def newMapperWithFastNumberParsing(): ObjectMapper = {
    JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
      .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
      .build()
  }
}
