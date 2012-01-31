package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptionDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {
  lazy val module = new JacksonModule with OptionDeserializerModule

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
}