package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner

object TestEnum extends Enumeration {
  type TestEnum = Value
  val Value1, Value2 = Value
}

class TestEnumClass extends TypeReference[TestEnum.type]

case class TestObject1(@JsonScalaEnumeration(classOf[TestEnumClass]) field: TestEnum.Value)

case class TestObject2(field: TestEnum.Value)

abstract class TestObject2Mixin {
  @JsonScalaEnumeration(classOf[TestEnumClass]) var field: TestEnum.Value = TestEnum.Value1
}

@RunWith(classOf[JUnitRunner])
class EnumMixinTest extends FlatSpec with Matchers {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.addMixin[TestObject2, TestObject2Mixin]()

  val json = """{"field": "Value1"}"""

  "An ObjectMapper with the ScalaObjectMapper mixin" should "handle JsonScalaEnumeration annotations for an enum" in {
    val obj = mapper.readValue[TestObject1](json)
    obj shouldEqual TestObject1(TestEnum.Value1)
  }

  it should "handle mixin annotations for an enum" in {
    val mixinResult = mapper.findMixInClassFor[TestObject2]
    mixinResult shouldEqual classOf[TestObject2Mixin]
    //val obj = mapper.readValue[TestObject2](json)
    //obj shouldEqual TestObject2(TestEnum.Value1)
  }
}
