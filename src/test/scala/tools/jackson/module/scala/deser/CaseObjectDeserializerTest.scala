package tools.jackson.module.scala.deser

import CaseObjectDeserializerTest.{Foo, Holder, TestObject}
import com.fasterxml.jackson.annotation.JsonAutoDetect
import tools.jackson.databind.DeserializationFeature
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

  case class Holder(obj: TestObject.type, after: Int)
}

class CaseObjectDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "deserialize a case object and not create a new instance" in {
    val mapper = newMapper
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    json shouldEqual "{}"
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized === original)
  }

  it should "deserialize a case object and not create a new instance (FAIL_ON_TRAILING_TOKENS disabled)" in {
    val mapper = newBuilder
      .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      .build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    json shouldEqual "{}"
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

  it should "leave the tokens of the enclosing object alone" in {
    val mapper = newMapper
    mapper.readValue("""{"obj":{},"after":7}""", classOf[Holder]) shouldEqual Holder(TestObject, 7)
  }

  it should "skip a nested object written where a case object is expected" in {
    val mapper = newMapper
    mapper.readValue("""{"obj":{"x":{"y":1}},"after":7}""", classOf[Holder]) shouldEqual Holder(TestObject, 7)
  }

  it should "skip a scalar written where a case object is expected" in {
    val mapper = newMapper
    mapper.readValue("""{"obj":5,"after":7}""", classOf[Holder]) shouldEqual Holder(TestObject, 7)
  }

  it should "not hang on a scalar read as a case object" in {
    // the read runs on its own thread: a regression here spins forever rather than failing, and the
    // suite has to be able to report that instead of hanging with it
    val mapper = newMapper
    @volatile var result: Any = null
    val reader = new Thread(() => result = mapper.readValue("5", TestObject.getClass))
    reader.setDaemon(true)
    reader.start()
    reader.join(30000)
    withClue("reading a scalar as a case object did not terminate: ") {
      reader.isAlive shouldBe false
    }
    result shouldEqual TestObject
  }

  "An ObjectMapper without ScalaObjectDeserializerModule" should "deserialize a case object but create a new instance" in {
    val mapper = JsonMapper.builder().addModule(ScalaAnnotationIntrospectorModule).build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized != original)
  }

}
