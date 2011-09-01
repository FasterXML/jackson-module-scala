package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.codehaus.jackson.`type`.TypeReference
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptionDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {
  lazy val module = new JacksonModule with OptionDeserializerModule

  "An ObjectMapper with OptionDeserializer" should "deserialize an Option[Int]" in {
    deserialize("1", new TypeReference[Option[Int]]{}) should be (Some(1))
    deserialize("1", new TypeReference[Option[Int]]{}) should be (Option(1))
    deserialize("null", new TypeReference[Option[Int]]{}) should be (None)
  }

  it should "deserialize an Option[String]" in {
    deserialize("\"foo\"", new TypeReference[Option[String]]{}) should be (Some("foo"))
    deserialize("\"foo\"", new TypeReference[Option[String]]{}) should be (Option("foo"))
    deserialize("null", new TypeReference[Option[String]]{}) should be (None)

  }
}