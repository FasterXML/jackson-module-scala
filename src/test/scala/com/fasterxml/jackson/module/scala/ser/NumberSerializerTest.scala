package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

class NumberSerializerTest extends SerializerTest {

  val module: JacksonModule = DefaultScalaModule

  "NumberSerializer" should "serialize BigDecimal as a number" in {
    serialize(BigDecimal("123.456")) shouldBe "123.456"
  }

  it should "serialize BigDecimal as a string ()" in {
    val mapper = newMapper
    mapper.configOverride(classOf[java.math.BigDecimal])
      .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING))
    serialize(BigDecimal("123.456"), mapper) shouldBe "123.456"
  }

  it should "serialize BigInt as a number" in {
    serialize(BigInt("12345678901234567890")) shouldBe "12345678901234567890"
  }

  it should "serialize Int as a number" in {
    serialize(42) shouldBe "42"
  }

  it should "serialize Long as a number" in {
    serialize(12345678901234L) shouldBe "12345678901234"
  }

}
