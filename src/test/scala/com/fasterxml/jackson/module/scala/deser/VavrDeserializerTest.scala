package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, EitherField, Impl, PlainPojoObject}

/**
  * Ensuring we can read serialized Vavr Either types into their equivalent Scala types.
  */
class VavrDeserializerTest extends DeserializerTest with EitherJsonTestSupport {

  val module: DefaultScalaModule.type = DefaultScalaModule

  "DefaultScalaModule" should "be able to deserialize right with string" in {
    deserialize(s"""["right", "$str"]""", classOf[Either[_, String]], Seq(classOf[Any], classOf[String])) should be (Right(str))
  }

  it should "be able to deserialize left with string" in {
    deserialize(s"""["left", "$str"]""", classOf[Either[String, _]], Seq(classOf[String], classOf[Any])) should be (Left(str))
  }

  it should "be able to deserialize right with null value" in {
    deserialize("""["right", null]""", classOf[Either[_, String]], Seq(classOf[Any], classOf[String])) should be (Right(null))
  }

  it should "be able to deserialize left with null value" in {
    deserialize("""["left", null]""", classOf[Either[String, String]], Seq(classOf[String], classOf[String])) should be (Left(null))
  }

  it should "be able to deserialize Right with complex objects" in {
    deserialize(s"""["right", ${serialize(obj)}]""", new TypeReference[Either[String, PlainPojoObject]]{}) should be (Right(obj))
  }

  it should "be able to deserialize Left with complex objects" in {
    deserialize(s"""["left", ${serialize(obj)}]""", new TypeReference[Either[PlainPojoObject, String]]{}) should be (Left(obj))
  }

  it should "propagate type information for Right" in {
    deserialize("""{"base":{"r":{"$type":"impl"}}}""", new TypeReference[BaseHolder]{}) should be(BaseHolder(Right(Impl())))
  }

  it should "propagate type information for Left" in {
    deserialize("""{"base":{"l":{"$type":"impl"}}}""", new TypeReference[BaseHolder]{}) should be(BaseHolder(Left(Impl())))
  }

  it should "deserialize a seq wrapped Either" in {
    deserialize("""[["left", "left"]]""", new TypeReference[Seq[Either[String, String]]]{}) shouldBe Seq(Left("left"))
  }

  it should "deserialize class with a field with Either" in {
    deserialize("""{"either":["right", {"a":"1","b":null,"c":1}]}""", new TypeReference[EitherField]{}) shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
  }
}