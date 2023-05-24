package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.annotation.meta.getter

object JsonValueTest {
  case class ValueClass(@(JsonValue @getter) value: String)
}

// does not work with Scala3 prior to 3.3.0
class JsonValueTest extends SerializerTest {
  import JsonValueTest._

  override def module: Module = DefaultScalaModule

  "DefaultScalaModule" should "support @JsonValue" in {
    serialize(ValueClass("Foo")) should equal (""""Foo"""")
  }
}
