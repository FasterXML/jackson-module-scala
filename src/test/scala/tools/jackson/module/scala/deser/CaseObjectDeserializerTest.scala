package tools.jackson.module.scala.deser

import CaseObjectDeserializerTest.{Foo, TestObject}
import com.fasterxml.jackson.annotation.JsonAutoDetect
import tools.jackson.databind.introspect.VisibilityChecker
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

import scala.compat.java8.FunctionConverters.asJavaUnaryOperator

object CaseObjectDeserializerTest {
  case object TestObject

  case object Foo {
    val field: String = "bar"
  }
}

class CaseObjectDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "deserialize a case object and not create a new instance" in {
    val mapper = newMapper
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized === original)
  }

  it should "deserialize Foo and not create a new instance" in {
    val mapper = newMapper
    val original = Foo
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, Foo.getClass)
    assert(deserialized === original)
  }

  it should "deserialize Foo and not create a new instance (visibility settings)" in {
    val mapper = newBuilder
      .changeDefaultVisibility(asJavaUnaryOperator((a: Any) => {
        VisibilityChecker.defaultInstance()
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      }))
      .build()
    val original = Foo
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, Foo.getClass)
    assert(deserialized === original)
  }

  "An ObjectMapper with ClassTagExtensions" should "deserialize a case object and not create a new instance" in {
    val mapper = newMapper :: ClassTagExtensions
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue[TestObject.type](json)
    assert(deserialized === original)
  }

  "An ObjectMapper without ScalaObjectDeserializerModule" should "deserialize a case object but create a new instance" in {
    val mapper = JsonMapper.builder().addModule(ScalaAnnotationIntrospectorModule).build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized != original)
  }

}
