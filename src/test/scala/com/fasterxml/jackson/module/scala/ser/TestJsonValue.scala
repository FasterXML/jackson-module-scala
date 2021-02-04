package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JacksonModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.annotation.meta.getter

object TestJsonValue {
  case class ValueClass(@(JsonValue @getter) value: String)
}

class TestJsonValue extends SerializerTest {
  import TestJsonValue._

  override def module: JacksonModule = DefaultScalaModule

  "DefaultScalaModule" should "support @JsonValue" in {
    serialize(ValueClass("Foo")) should equal (""""Foo"""")
  }
}
