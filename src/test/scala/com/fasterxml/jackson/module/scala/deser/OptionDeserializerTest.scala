package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonSetter, JsonSubTypes, JsonTypeInfo, JsonTypeName, Nulls}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.annotation.meta.field

case class UnavailableField(foo: Option[String])
case class JavaOptionalWrapper(o: java.util.Optional[String])
case class OptionWrapper(o: Option[String])

object OptionDeserializerTest {
  @JsonSubTypes(Array(new JsonSubTypes.Type(classOf[Impl])))
  trait Base

  @JsonTypeName("impl")
  case class Impl() extends Base

  case class BaseHolder(
    private var _base: Option[Base]
  ) {
    @(JsonTypeInfo @field)(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="$type")
    def base: Option[Base] = _base
    def base_=(base:Option[Base]): Unit = { _base = base }
  }

  case class Defaulted(id: Int, name: String = "") {
    def this() = this(1,"")
  }

  case class Foo(bar: String)
  case class Wrapper[T](t: T)
}

@RunWith(classOf[JUnitRunner])
class OptionDeserializerTest extends DeserializerTest {
  import OptionDeserializerTest._
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with OptionDeserializer" should "deserialize an Option[Int]" in {
    deserialize[Option[Int]]("1") should be (Some(1))
    deserialize[Option[Int]]("1") should be (Option(1))
    deserialize[Option[Int]]("null") should be (None)
  }

  it should "deserialize an Option[String]" in {
    deserialize[Option[String]]("\"foo\"") should be (Some("foo"))
    deserialize[Option[String]]("\"foo\"") should be (Option("foo"))
    deserialize[Option[String]]("null") should be (None)
  }

  it should "deserialize an Option[Long] to a long" in {
    deserialize[Option[Long]]("123456789012345678") should be (Some(123456789012345678L))
    deserialize[Option[Long]]("123456789012345678").map(java.lang.Long.valueOf(_)) should be (Some(123456789012345678L))
    deserialize[Option[Long]]("123456789012345678").get.getClass should be (classOf[Long])

    deserialize[Option[Long]]("1") should be (Some(1L))
    deserialize[Option[Long]]("1").map(java.lang.Long.valueOf(_)) should be (Some(1L))
    deserialize[Option[Long]]("1").get.getClass should be (classOf[Long])
  }

  it should "sythensize None for optional fields that are non-existent" in {
    deserialize[UnavailableField]("{}") should be(UnavailableField(None))
  }

  it should "propagate type information" in {
    val json: String = """{"base":{"$type":"impl"}}"""
    deserialize[BaseHolder](json) should be(BaseHolder(Some(Impl())))
  }

  it should "deserialize a polymorphic null as None" in {
    deserialize[BaseHolder]("""{"base":null}""") should be(BaseHolder(None))
  }

  it should "deserialize defaulted parameters correctly (without defaults)" in {
    val json = newMapper.writeValueAsString(Defaulted(id = 1))
    json shouldBe """{"id":1,"name":""}"""
    val d = newMapper.readValue(json, classOf[Defaulted])
    d.name should not be null
  }

  it should "deserialize a type param wrapped option" in {
    val json: String = """{"t": {"bar": "baz"}}"""
    val result = deserialize[Wrapper[Option[Foo]]](json)
    result.t.get.isInstanceOf[Foo] should be(true)
  }

  it should "handle AS_NULL" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    mapper.registerModule(new Jdk8Module())
    mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
    val json = """{"o": null}"""
    val result1 = mapper.readValue(json, classOf[JavaOptionalWrapper])
    result1 shouldEqual JavaOptionalWrapper(java.util.Optional.empty[String]())
    val result2 = mapper.readValue(json, classOf[OptionWrapper])
    result2 shouldEqual OptionWrapper(None)
  }
}
