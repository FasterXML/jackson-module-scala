package com.fasterxml.jackson.module.scala.deser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.fasterxml.jackson.module.scala.JacksonModule
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.JsonMappingException

case class CaseClassConstructorTest(intValue: Int, stringValue: String) {

}

case class CaseClassPropertiesTest() {
  var intProperty: Int = 0
  var stringProperty: String = null
}

case class CaseClassJacksonAnnotationTest(@JsonProperty("foo") oof:String, bar: String) {

}

@RunWith(classOf[JUnitRunner])
class CaseClassDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  def module = new JacksonModule with CaseClassDeserializerModule

  "An ObjectMapper with CaseClassDeserializer" should "deserialize a case class with a single constructor" in {
    deserialize[CaseClassConstructorTest]("""{"intValue":1,"stringValue":"foo"}""") should be (CaseClassConstructorTest(1,"foo"))
  }

  it should "deserialize a case class with var properties" in {
    val result = CaseClassPropertiesTest()
    result.intProperty = 1
    result.stringProperty = "foo"
    deserialize[CaseClassPropertiesTest]("""{"intProperty":1,"stringProperty":"foo"}""") should be (result)
  }

  it should "honor Jackson annotations" in {
    val result = CaseClassJacksonAnnotationTest("foo","bar")
    deserialize[CaseClassJacksonAnnotationTest]("""{"foo":"foo","bar":"bar"}""") should be (result)
  }

  it should "not try to deserialize a List" in {
    intercept[JsonMappingException] {
      deserialize[List[_]]("""{"foo":"foo","bar":"bar"}""")
    }
  }
}