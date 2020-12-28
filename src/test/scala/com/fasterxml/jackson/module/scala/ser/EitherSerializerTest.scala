package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, Impl}
import com.fasterxml.jackson.module.scala.deser.EitherJsonTestSupport
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

class EitherSerializerTest extends SerializerTest with EitherJsonTestSupport {

  val module: JacksonModule = DefaultScalaModule

  val json: JsonNode = jsonOf(s"""{"prop":"$str"}""")

  "EitherSerializer" should "be able to serialize right with string" in {
    serialize(Right(str)) should be (s"""{"r":"$str"}""")
  }

  it should "be able to serialize left with string" in {
    serialize(Left(str)) should be (s"""{"l":"$str"}""")
  }

  it should "be able to serialize right with null value" in {
    serialize(Right(null)) should be (s"""{"r":null}""")
  }

  it should "be able to serialize left with null value" in {
    serialize(Left(null)) should be (s"""{"l":null}""")
  }

  it should "be able to serialize Right with complex objects" in {
    serialize(Right(obj)) should be (s"""{"r":${serialize(obj)}}""")
  }

  it should "be able to serialize Left with complex objects" in {
    serialize(Left(obj)) should be (s"""{"l":${serialize(obj)}}""")
  }

  it should "serialize contained JsonNode in Right correctly" in {
    serialize(WrapperOfEitherOfJsonNode(Right(json))) shouldBe s"""{"either":{"r":$json}}"""
  }

  it should "serialize contained JsonNode in Left correctly" in {
    serialize(WrapperOfEitherOfJsonNode(Left(json))) shouldBe s"""{"either":{"l":$json}}"""
  }

  it should "propagate type information on Right" in {
    serialize(BaseHolder(Right(Impl()))) shouldBe """{"base":{"r":{"$type":"impl"}}}"""
  }

  it should "propagate type information on Left" in {
    serialize(BaseHolder(Left(Impl()))) shouldBe """{"base":{"l":{"$type":"impl"}}}"""
  }

  it should "properly serialize null when using type info" in {
    serialize(BaseHolder(null)) shouldBe """{"base":null}"""
  }
}
