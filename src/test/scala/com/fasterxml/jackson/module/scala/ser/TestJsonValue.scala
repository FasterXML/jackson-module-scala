package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.annotation.JsonValue
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import scala.annotation.target.getter

object TestJsonValue
{
  case class ValueClass(@(JsonValue @getter) value: String)
}

@RunWith(classOf[JUnitRunner])
class TestJsonValue extends SerializerTest {
  import TestJsonValue._

  override def module: Module = DefaultScalaModule

  "DefaultScalaModule" should "support @JsonValue" in {
    serialize(ValueClass("Foo")) should equal (""""Foo"""")
  }
}
