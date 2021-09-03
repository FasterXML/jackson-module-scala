package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import com.fasterxml.jackson.module.scala.deser.CaseObjectDeserializerTest.TestObject

object CaseObjectDeserializerTest {
  case object TestObject
}

class CaseObjectDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule and ScalaObjectDeserializerModule" should "deserialize a case object and not create a new instance" in {
    val mapper = newMapper
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized == original)
  }

  "An ObjectMapper with ClassTagExtensions and ScalaObjectDeserializerModule" should "deserialize a case object and not create a new instance" in {
    val mapper = newMapper :: ClassTagExtensions
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue[TestObject.type](json)
    assert(deserialized == original)
  }

}
