package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.junit.runner.RunWith
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
class EnumMixinTest extends BaseSpec {
  val baseBuilder = JsonMapper.builder().addModule(new DefaultScalaModule).addMixIn(classOf[TestObject2], classOf[TestObject2Mixin])
  val mapper = baseBuilder.build() :: ScalaObjectMapper

  val json = """{"field": "Value1"}"""

  "An ObjectMapper with the ScalaObjectMapper mixin" should "handle JsonScalaEnumeration annotations for an enum" in {
    val obj = mapper.readValue[TestObject1](json)
    obj shouldEqual TestObject1(TestEnum.Value1)
  }

//TODO fix
//  it should "handle mixin annotations for an enum" in {
//    val mixinResult = mapper.findMixInClassFor[TestObject2]
//    mixinResult shouldEqual classOf[TestObject2Mixin]
//    val obj = mapper.readValue[TestObject2](json)
//    obj shouldEqual TestObject2(TestEnum.Value1)
//  }

  it should "handle mixin annotations for an enum (case class mixin)" in {
    val baseMapper = JsonMapper.builder().addModule(new DefaultScalaModule)
      .addMixIn(classOf[TestObject2], classOf[TestObject1]).build()
    val m1 = baseMapper :: ScalaObjectMapper
//TODO fix
//    val mixinResult = m1.findMixInClassFor[TestObject2]
//    mixinResult shouldEqual classOf[TestObject1]
//    val obj = m1.readValue[TestObject2](json)
//    obj shouldEqual TestObject2(TestEnum.Value1)
  }
}
