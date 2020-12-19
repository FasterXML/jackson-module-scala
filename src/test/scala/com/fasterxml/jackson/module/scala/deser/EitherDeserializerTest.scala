package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, EitherField, Impl, PlainPojoObject}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.annotation.meta.field
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class EitherDeserializerTest extends DeserializerTest with EitherJsonTestSupport {

  override val module = DefaultScalaModule

  "DefaultScalaModule" should "be able to deserialize right with string" in {
    deserializeWithManifest[Either[_, String]](s"""{"r":"$str"}""") should be (Right(str))
    deserializeWithManifest[Either[_, String]](s"""{"right":"$str"}""") should be (Right(str))
  }

  it should "be able to deserialize left with string" in {
    deserializeWithManifest[Either[String, _]](s"""{"l":"$str"}""") should be (Left(str))
    deserializeWithManifest[Either[String, _]](s"""{"left":"$str"}""") should be (Left(str))
  }

  it should "be able to deserialize right with null value" in {
    deserializeWithManifest[Either[_, String]](s"""{"r":null}""") should be (Right(null))
    deserializeWithManifest[Either[_, String]](s"""{"right":null}""") should be (Right(null))
  }

  it should "be able to deserialize right with None value" in {
    deserializeWithManifest[Either[_, Option[String]]](s"""{"r":null}""") should be (Right(None))
    deserializeWithManifest[Either[_, Option[String]]](s"""{"right":null}""") should be (Right(None))
  }

  it should "be able to deserialize left with null value" in {
    deserializeWithManifest[Either[String, String]](s"""{"l":null}""") should be (Left(null))
    deserializeWithManifest[Either[String, String]](s"""{"left":null}""") should be (Left(null))
  }

  it should "be able to deserialize left with None value" in {
    deserializeWithManifest[Either[Option[String], _]](s"""{"l":null}""") should be (Left(None))
    deserializeWithManifest[Either[Option[String], _]](s"""{"left":null}""") should be (Left(None))
  }

  it should "be able to deserialize Right with complex objects" in {
    deserializeWithManifest[Either[String, PlainPojoObject]](s"""{"r":${serialize(obj)}}""") should be (Right(obj))
    deserializeWithManifest[Either[String, PlainPojoObject]](s"""{"right":${serialize(obj)}}""") should be (Right(obj))
  }

  it should "be able to deserialize Left with complex objects" in {
    deserializeWithManifest[Either[PlainPojoObject, String]](s"""{"l":${serialize(obj)}}""") should be (Left(obj))
    deserializeWithManifest[Either[PlainPojoObject, String]](s"""{"left":${serialize(obj)}}""") should be (Left(obj))
  }

  it should "propagate type information for Right" in {
    deserializeWithManifest[BaseHolder]("""{"base":{"r":{"$type":"impl"}}}""") should be(BaseHolder(Right(Impl())))
    deserializeWithManifest[BaseHolder]("""{"base":{"right":{"$type":"impl"}}}""") should be(BaseHolder(Right(Impl())))
  }

  it should "propagate type information for Left" in {
    deserializeWithManifest[BaseHolder]("""{"base":{"l":{"$type":"impl"}}}""") should be(BaseHolder(Left(Impl())))
    deserializeWithManifest[BaseHolder]("""{"base":{"left":{"$type":"impl"}}}""") should be(BaseHolder(Left(Impl())))
  }

  it should "deserialize a polymorphic null as null" in {
    deserializeWithManifest[BaseHolder]("""{"base":null}""") should be(BaseHolder(null))
  }

  it should "deserialize a seq wrapped Either" in {
    deserializeWithManifest[Seq[Either[String, String]]]("""[{"l":"left"}]""") shouldBe Seq(Left("left"))
    deserializeWithManifest[Seq[Either[String, String]]]("""[{"left":"left"}]""") shouldBe Seq(Left("left"))
  }

  it should "deserialize class with a field with Either" in {
    deserializeWithManifest[EitherField]("""{"either":{"r":{"a":"1","b":null,"c":1}}}""") shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
    deserializeWithManifest[EitherField]("""{"either":{"right":{"a":"1","b":null,"c":1}}}""") shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
  }
}


trait EitherJsonTestSupport {

  val str: String = randomStr
  val obj = PlainPojoObject(randomStr, randomStrOpt, Random.nextLong())


  private def randomStr: String = Random.alphanumeric.take(20).mkString
  private def randomStrOpt = Some(randomStr)


  case class WrapperOfEitherOfJsonNode(either: Either[JsonNode, JsonNode])

}

object EitherJsonTest {

  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[Impl])))
  trait Base

  @JsonTypeName("impl")
  case class Impl() extends Base

  case class BaseHolder(private var _base: Either[Base, Base]) {
    @(JsonTypeInfo @field)(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="$type")
    def base: Either[Base, Base] = _base
    def base_=(base:Either[Base, Base]): Unit = { _base = base }
  }

  case class EitherField(either: Either[Int,PlainPojoObject])

  case class PlainPojoObject(a: String, b: Option[String], c: Long)
}

