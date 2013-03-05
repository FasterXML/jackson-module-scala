package com.fasterxml.jackson.module.scala.deser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class UnavailableField(foo: Option[String])

@RunWith(classOf[JUnitRunner])
class OptionDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {
  lazy val module = DefaultScalaModule

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
}
