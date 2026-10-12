package tools.jackson.module.scala.deser

import CaseObjectDeserializerTest.{Foo, Holder, TestObject}
import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonTypeInfo}
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.introspect.VisibilityChecker
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

import scala.collection.mutable

object CaseObjectDeserializerTest {
  case object TestObject

  case object Foo {
    val field: String = "bar"
  }

  case class Holder(obj: TestObject.type, after: Int)

  // objects holding mutable state: reading one returns the singleton and leaves its state alone
  case object Counter {
    var count: Int = 0
  }

  object PlainCounter {
    var count: Int = 0
  }

  case object Registry {
    val names: mutable.ListBuffer[String] = mutable.ListBuffer.empty
  }

  case class CounterHolder(counter: Counter.type, after: Int)

  // a case object with properties of its own, inherited from a base whose type id is written as a
  // property: after reading the id the type deserializer hands over the parser inside the object
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
  sealed abstract class Shape(val sides: Int, val meta: Map[String, Int])
  case object Point extends Shape(0, Map("dim" -> 0))
  case class Drawing(shape: Shape, after: Int)
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
      .changeDefaultVisibility(_ => {
        VisibilityChecker.defaultInstance()
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
      })
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

  "An ObjectMapper with DefaultScalaModule and an object holding mutable state" should "return the case object without setting its var from the JSON" in {
    import CaseObjectDeserializerTest.Counter
    try {
      Counter.count = 5
      val deserialized = newMapper.readValue("""{"count":99}""", Counter.getClass)
      assert(deserialized eq Counter)
      Counter.count shouldEqual 5
    } finally {
      Counter.count = 0
    }
  }

  it should "return the plain object without setting its var from the JSON" in {
    import CaseObjectDeserializerTest.PlainCounter
    try {
      PlainCounter.count = 3
      val deserialized = newMapper.readValue("""{"count":99}""", PlainCounter.getClass)
      assert(deserialized eq PlainCounter)
      PlainCounter.count shouldEqual 3
    } finally {
      PlainCounter.count = 0
    }
  }

  it should "leave the contents of a mutable collection in the object alone" in {
    import CaseObjectDeserializerTest.Registry
    try {
      Registry.names += "a"
      val deserialized = newMapper.readValue("""{"names":["x","y"]}""", Registry.getClass)
      assert(deserialized eq Registry)
      Registry.names shouldEqual mutable.ListBuffer("a")
    } finally {
      Registry.names.clear()
    }
  }

  it should "leave the var alone when the object is read as a property of a case class" in {
    import CaseObjectDeserializerTest.{Counter, CounterHolder}
    try {
      Counter.count = 5
      val deserialized = newMapper.readValue("""{"counter":{"count":42},"after":7}""", classOf[CounterHolder])
      deserialized shouldEqual CounterHolder(Counter, 7)
      assert(deserialized.counter eq Counter)
      Counter.count shouldEqual 5
    } finally {
      Counter.count = 0
    }
  }

  it should "roundtrip the case object to the singleton, whatever its state" in {
    import CaseObjectDeserializerTest.Counter
    try {
      Counter.count = 5
      val mapper = newMapper
      val json = mapper.writeValueAsString(Counter)
      json shouldEqual """{"count":5}"""
      Counter.count = 6
      val deserialized = mapper.readValue(json, Counter.getClass)
      assert(deserialized eq Counter)
      Counter.count shouldEqual 6
    } finally {
      Counter.count = 0
    }
  }

  "An ObjectMapper without ScalaObjectDeserializerModule" should "deserialize a case object but create a new instance" in {
    val mapper = JsonMapper.builder().addModule(ScalaAnnotationIntrospectorModule).build()
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue(json, TestObject.getClass)
    assert(deserialized != original)
  }

  "An ObjectMapper with DefaultScalaModule and an As.PROPERTY type id" should "deserialize a case object with properties whose type id is a property" in {
    import CaseObjectDeserializerTest.{Drawing, Point}
    val mapper = newMapper
    val json = mapper.writeValueAsString(Drawing(Point, 1))
    json shouldEqual """{"shape":{"@class":"tools.jackson.module.scala.deser.CaseObjectDeserializerTest$Point$","sides":0,"meta":{"dim":0}},"after":1}"""
    val deserialized = mapper.readValue(json, classOf[Drawing])
    deserialized shouldEqual Drawing(Point, 1)
    assert(deserialized.shape eq Point)
  }

  it should "deserialize a case object with properties whose type id is a property but not the first one" in {
    // https://github.com/FasterXML/jackson-module-scala/issues/899
    // the properties ahead of the type id are buffered and replayed, a different route into the deserializer
    import CaseObjectDeserializerTest.{Drawing, Point}
    val mapper = newMapper
    val json = """{"shape":{"sides":0,"meta":{"dim":0},"@class":"tools.jackson.module.scala.deser.CaseObjectDeserializerTest$Point$"},"after":1}"""
    val deserialized = mapper.readValue(json, classOf[Drawing])
    deserialized shouldEqual Drawing(Point, 1)
    assert(deserialized.shape eq Point)
  }
}
