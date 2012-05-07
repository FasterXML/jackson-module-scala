package com.fasterxml.jackson.module.scala.deser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.fasterxml.jackson.module.scala.JacksonModule
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.JsonMappingException

case class ConstructorTestCaseClass(intValue: Int, stringValue: String)

case class PropertiesTestCaseClass() {
  var intProperty: Int = 0
  var stringProperty: String = null
}

case class JacksonAnnotationTestCaseClass(@JsonProperty("foo") oof:String, bar: String)

case class GenericTestCaseClass[T](data: T)

@RunWith(classOf[JUnitRunner])
class CaseClassDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  def module = new JacksonModule with CaseClassDeserializerModule

  "An ObjectMapper with CaseClassDeserializer" should "deserialize a case class with a single constructor" in {
    deserialize[ConstructorTestCaseClass]("""{"intValue":1,"stringValue":"foo"}""") should be (ConstructorTestCaseClass(1,"foo"))
  }

  it should "deserialize a case class with var properties" in {
    val result = PropertiesTestCaseClass()
    result.intProperty = 1
    result.stringProperty = "foo"
    deserialize[PropertiesTestCaseClass]("""{"intProperty":1,"stringProperty":"foo"}""") should be (result)
  }

  it should "honor Jackson annotations" in {
    val result = JacksonAnnotationTestCaseClass("foo","bar")
    deserialize[JacksonAnnotationTestCaseClass]("""{"foo":"foo","bar":"bar"}""") should be (result)
  }

  it should "not try to deserialize a List" in {
    intercept[JsonMappingException] {
      deserialize[List[_]]("""{"foo":"foo","bar":"bar"}""")
    }
  }

  it should "deserialize a generic case class" in {
    val result = GenericTestCaseClass(42)
    deserialize[GenericTestCaseClass[Int]]("""{"data":42}""") should be (result)
  }
}