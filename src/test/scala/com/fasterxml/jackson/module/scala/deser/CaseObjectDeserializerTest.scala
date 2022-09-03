package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import com.fasterxml.jackson.module.scala.deser.CaseObjectDeserializerTest.{Foo, TestObject}

object CaseObjectDeserializerTest {
  case object TestObject

  case object Foo {
    val field: String = "bar"
  }
}

//see also CaseObjectScala2DeserializerTest
class CaseObjectDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule and ScalaObjectDeserializerModule" should "deserialize a case object and not create a new instance" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).addModule(ScalaObjectDeserializerModule).build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized == original)
  }

  it should "deserialize Foo and not create a new instance" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).addModule(ScalaObjectDeserializerModule).build()
    val original = Foo
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, Foo.getClass)
    assert(deserialized == original)
  }

  it should "deserialize Foo and not create a new instance (visibility settings)" in {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(ScalaObjectDeserializerModule)
      .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
      .build()
    val original = Foo
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, Foo.getClass)
    assert(deserialized == original)
  }

  "An ObjectMapper with ClassTagExtensions and ScalaObjectDeserializerModule" should "deserialize a case object and not create a new instance" in {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(ScalaObjectDeserializerModule)
      .build() :: ClassTagExtensions
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue[TestObject.type](json)
    assert(deserialized == original)
  }

  "An ObjectMapper with DefaultScalaModule but not ScalaObjectDeserializerModule" should "deserialize a case object but create a new instance" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized != original)
  }

}
