package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTest.{BaseHolder, EitherField, Impl, PlainPojoObject}

import scala.annotation.meta.field
import scala.util.Random

class EitherDeserializerTest extends DeserializerTest with EitherJsonTestSupport {

  override val module = DefaultScalaModule

  "DefaultScalaModule" should "be able to deserialize right with string" in {
    val typeRef = new TypeReference[Either[_, String]] {}
    deserialize(s"""{"r":"$str"}""", typeRef) should be (Right(str))
    deserialize(s"""{"right":"$str"}""", typeRef) should be (Right(str))
  }

  it should "be able to deserialize left with string" in {
    val typeRef = new TypeReference[Either[_, String]] {}
    deserialize(s"""{"l":"$str"}""", typeRef) should be (Left(str))
    deserialize(s"""{"left":"$str"}""", typeRef) should be (Left(str))
  }

  it should "be able to deserialize right with null value" in {
    val typeRef = new TypeReference[Either[_, String]] {}
    deserialize(s"""{"r":null}""", typeRef) should be (Right(null))
    deserialize(s"""{"right":null}""", typeRef) should be (Right(null))
  }

  it should "be able to deserialize right with None value" in {
    val typeRef = new TypeReference[Either[_, Option[String]]] {}
    deserialize(s"""{"r":null}""", typeRef) should be (Right(None))
    deserialize(s"""{"right":null}""", typeRef) should be (Right(None))
  }

  it should "be able to deserialize left with null value" in {
    val typeRef = new TypeReference[Either[String, String]] {}
    deserialize(s"""{"l":null}""", typeRef) should be (Left(null))
    deserialize(s"""{"left":null}""", typeRef) should be (Left(null))
  }

  it should "be able to deserialize left with None value" in {
    val typeRef = new TypeReference[Either[Option[String], _]] {}
    deserialize(s"""{"l":null}""", typeRef) should be (Left(None))
    deserialize(s"""{"left":null}""", typeRef) should be (Left(None))
  }

  it should "be able to deserialize Right with complex objects" in {
    val typeRef = new TypeReference[Either[String, PlainPojoObject]] {}
    deserialize(s"""{"r":${serialize(obj)}}""", typeRef) should be (Right(obj))
    deserialize(s"""{"right":${serialize(obj)}}""", typeRef) should be (Right(obj))
  }

  it should "be able to deserialize Left with complex objects" in {
    val typeRef = new TypeReference[Either[PlainPojoObject, String]] {}
    deserialize(s"""{"l":${serialize(obj)}}""", typeRef) should be (Left(obj))
    deserialize(s"""{"left":${serialize(obj)}}""", typeRef) should be (Left(obj))
  }

  it should "propagate type information for Right" in {
    val typeRef = new TypeReference[BaseHolder] {}
    deserialize("""{"base":{"r":{"$type":"impl"}}}""", typeRef) should be(BaseHolder(Right(Impl())))
    deserialize("""{"base":{"right":{"$type":"impl"}}}""", typeRef) should be(BaseHolder(Right(Impl())))
  }

  it should "propagate type information for Left" in {
    val typeRef = new TypeReference[BaseHolder] {}
    deserialize("""{"base":{"l":{"$type":"impl"}}}""", typeRef) should be(BaseHolder(Left(Impl())))
    deserialize("""{"base":{"left":{"$type":"impl"}}}""", typeRef) should be(BaseHolder(Left(Impl())))
  }

  it should "deserialize a polymorphic null as null" in {
    val typeRef = new TypeReference[BaseHolder] {}
    deserialize("""{"base":null}""", typeRef) should be(BaseHolder(null))
  }

  it should "deserialize a seq wrapped Either" in {
    val typeRef = new TypeReference[Seq[Either[String, String]]] {}
    deserialize("""[{"l":"left"}]""", typeRef) shouldBe Seq(Left("left"))
    deserialize("""[{"left":"left"}]""", typeRef) shouldBe Seq(Left("left"))
  }

  it should "deserialize class with a field with Either" in {
    val typeRef = new TypeReference[EitherField] {}
    deserialize("""{"either":{"r":{"a":"1","b":null,"c":1}}}""", typeRef) shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
    deserialize("""{"either":{"right":{"a":"1","b":null,"c":1}}}""", typeRef) shouldBe EitherField(Right(PlainPojoObject("1", None, 1)))
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

