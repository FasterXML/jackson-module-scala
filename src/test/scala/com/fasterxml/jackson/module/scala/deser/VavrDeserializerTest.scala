package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, EitherField, Impl, PlainPojoObject}

/**
  * Ensuring we can read serialized Vavr Either types into their equivalent Scala types.
  */
class VavrDeserializerTest extends DeserializerTest with EitherJsonTestSupport {

  val module: DefaultScalaModule.type = DefaultScalaModule

  "DefaultScalaModule" should "be able to deserialize right with string" in {
    deserialize[Either[_, String]](s"""["right", $str]""")
  }

  it should "be able to deserialize left with string" in {
    deserialize[Either[String, _]](s"""["left", $str]""") should be (Left(str))
  }

  it should "be able to deserialize right with null value" in {
    deserialize[Either[_, String]]("""["right", null]""") should be (Right(null))
  }

  it should "be able to deserialize left with null value" in {
    deserialize[Either[String, String]]("""["left", null]""") should be (Left(null))
  }

  it should "be able to deserialize Right with complex objects" in {
    deserialize[Either[String, PlainPojoObject]](s"""["right", ${serialize(obj)}]""") should be (Right(obj))
  }

  it should "be able to deserialize Left with complex objects" in {
    deserialize[Either[PlainPojoObject, String]](s"""["left", ${serialize(obj)}]""") should be (Left(obj))
  }

  it should "propagate type information for Right" in {
    deserialize[BaseHolder](s"""["right", ${serialize(Impl())}]""") should be(BaseHolder(Right(Impl())))
  }

  it should "propagate type information for Left" in {
    deserialize[BaseHolder](s"""["left", ${serialize(Impl())}]""") should be(BaseHolder(Left(Impl())))
  }

  it should "deserialize a seq wrapped Either" in {
    deserialize[Seq[Either[String, String]]]("""[["left", "left"]]""") shouldBe Seq(Left("left"))
  }

  it should "deserialize class with a field with Either" in {
    deserialize[EitherField]("""{"either":["right", {"a":"1","b":null,"c":1}}}""") shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
  }
}