package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

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

class EnumMixinTest extends BaseSpec {
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
    val obj = mapper.readValue[TestObject2](json)
    obj shouldEqual TestObject2(TestEnum.Value1)
  }

  it should "handle mixin annotations for an enum (case class mixin)" in {
    val m1 = new ObjectMapper() with ScalaObjectMapper
    m1.registerModule(DefaultScalaModule)
    m1.addMixin[TestObject2, TestObject1]()
    val mixinResult = m1.findMixInClassFor[TestObject2]
    mixinResult shouldEqual classOf[TestObject1]
    val obj = m1.readValue[TestObject2](json)
    obj shouldEqual TestObject2(TestEnum.Value1)
  }
}
