package com.fasterxml.jackson.module.scala.deser

import javaslang.jackson.datatype.JavaslangModule

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, Impl, PlainPojoObject}

/**
  * Ensuring we can read serialized Javaslang types into their equivalent Scala types.
  */
class JavaslangDeserializerTest extends DeserializerTest with EitherJsonTestSupport {

  override val module = DefaultScalaModule

  val javaslangMapper: ObjectMapper =
    (new ObjectMapper)
      .registerModule(new JavaslangModule)
      .registerModule(DefaultScalaModule)

  override def serialize(o: AnyRef): String = javaslangMapper.writeValueAsString(o)

  "DefaultScalaModule" should "be able to deserialize right with string" in {
    deserialize[Either[_, String]](serialize(javaslang.control.Either.right(str))) should be (Right(str))
  }

  it should "be able to deserialize left with string" in {
    deserialize[Either[String, _]](serialize(javaslang.control.Either.left(str))) should be (Left(str))
  }

  it should "be able to deserialize right with null value" in {
    deserialize[Either[_, String]](serialize(javaslang.control.Either.right(null))) should be (Right(null))
  }

  it should "be able to deserialize left with null value" in {
    deserialize[Either[String, String]](serialize(javaslang.control.Either.left(null))) should be (Left(null))
  }

  it should "be able to deserialize Right with complex objects" in {
    deserialize[Either[String, PlainPojoObject]](serialize(javaslang.control.Either.right(obj))) should be (Right(obj))
  }

  it should "be able to deserialize Left with complex objects" in {
    deserialize[Either[PlainPojoObject, String]](serialize(javaslang.control.Either.left(obj))) should be (Left(obj))
  }

  it should "propagate type information for Right" in {
    deserialize[BaseHolder](serialize(BaseHolder(Right(Impl())))) should be(BaseHolder(Right(Impl())))
  }

  it should "propagate type information for Left" in {
    deserialize[BaseHolder](serialize(BaseHolder(Left(Impl())))) should be(BaseHolder(Left(Impl())))
  }

  it should "deserialize a polymorphic null as null" in {
    deserialize[BaseHolder]("""{"base":null}""") should be(BaseHolder(null))
  }

  it should "deserialize a seq wrapped Either" in {
    deserialize[Seq[Either[String, String]]](serialize(Seq(javaslang.control.Either.left("left")))) shouldBe Seq(Left("left"))
  }
}