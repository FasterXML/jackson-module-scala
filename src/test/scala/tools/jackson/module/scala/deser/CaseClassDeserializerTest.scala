package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonProperty, JsonSetter, Nulls}
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{DatabindException, DeserializationFeature, ObjectMapper, ObjectReader, PropertyNamingStrategies}
import tools.jackson.module.scala.DefaultScalaModule
import tools.jackson.module.scala.ser.{ClassWithOnlyUnitField, ClassWithUnitField}

import java.time.LocalDateTime

object CaseClassDeserializerTest {
  class Bean(var prop: String)

  case class Time(hour: String, minute: String)

  case class ConstructorTestCaseClass(intValue: Int, stringValue: String)

  case class PropertiesTestCaseClass() {
    var intProperty: Int = 0
    var stringProperty: String = _
  }

  case class JacksonAnnotationTestCaseClass(@JsonProperty("foo") oof:String, bar: String)

  case class GenericTestCaseClass[T](data: T)

  case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

  case class MixedPropertyNameStyleCaseClass(camelCase: Int, snake_case: Int, alllower: Int, ALLUPPER: Int, anID: Int)

  case class LongValueCaseClass(id: Long,
                                big: Option[Long],
                                @JsonDeserialize(contentAs = classOf[java.lang.Long])
                                small: Option[Long])

  class LongValueClass {
    @JsonDeserialize(contentAs = classOf[java.lang.Long])
    var small: Option[Long] = None
  }

  case class ArrayHolder(value: Array[Byte])

  case class MetricPath(path: String, level: Int, isRoot: Boolean) {
    def this(path: String) = this(path, 0, false)
  }

  case class Metric(path: MetricPath,
                    value: Double = 0,
                    time: String = LocalDateTime.now().toString,
                    tags: Set[String] = Set.empty) {

    def this(path: MetricPath) = this(path, 0, LocalDateTime.now().toString, Set())
    def this(path: MetricPath, value: Double) = this(path, value, LocalDateTime.now().toString, Set())
    def this(path: MetricPath, value: Double, time: String) = this(path, value, time, Set())
    def this(path: String) = this(new MetricPath(path), 0, LocalDateTime.now().toString, Set())
    def this(path: String, value: Double) = this(new MetricPath(path), value, LocalDateTime.now().toString, Set())
    def this(path: String, value: Double, time: String) = this(new MetricPath(path), value, time, Set())
  }

  case class Person(id: Int, name: String = "") {
    def this() = this(1, "")
  }

  case class NestedA(b: NestedB)

  //https://github.com/FasterXML/jackson-module-scala/issues/404
  class NestedB(id: Int) {
    def x = id
  }

  case class ListHolder[T](list: List[T])
  case class AnnotatedListHolder[T](@JsonSetter(nulls = Nulls.AS_EMPTY)list: List[T])
  case class OptionListHolder[T](list: Option[List[T]])

  case class MapHolder[K, V](map: Map[K, V])
  case class AnnotatedMapHolder[K, V](@JsonSetter(nulls = Nulls.AS_EMPTY)map: Map[K, V])
}

class CaseClassDeserializerTest extends DeserializerTest {
  import CaseClassDeserializerTest._

  def module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with CaseClassDeserializer" should "deserialize a case class with a single constructor" in {
    deserialize("""{"intValue":1,"stringValue":"foo"}""", classOf[ConstructorTestCaseClass]) should be (ConstructorTestCaseClass(1,"foo"))
  }

  it should "deserialize Nested case class" in {
    deserialize("""{"b":{"id":1}}""", classOf[NestedA]).b.x shouldBe 1
  }

  it should "deserialize a case class with multiple constructors (Metric)" in {
    val json = """{"path":{"path":"/path","level":1,"isRoot":false},"value":0.5,"time":"2017-05-10T00:00:00.000+02:00","tags":[]}"""
    deserialize(json, classOf[Metric]) shouldBe Metric(MetricPath("/path", 1, false), 0.5, "2017-05-10T00:00:00.000+02:00")
  }

  it should "deserialize a case class with multiple constructors (Person)" in {
    val result = deserialize("""{"id":1}""", classOf[Person])
    result shouldEqual Person(1, "")
  }

  it should "deserialize a case class with var properties" in {
    val result = PropertiesTestCaseClass()
    result.intProperty = 1
    result.stringProperty = "foo"
    deserialize("""{"intProperty":1,"stringProperty":"foo"}""", classOf[PropertiesTestCaseClass]) shouldBe result
  }

  it should "honor Jackson annotations" in {
    val result = JacksonAnnotationTestCaseClass("foo","bar")
    deserialize("""{"foo":"foo","bar":"bar"}""", classOf[JacksonAnnotationTestCaseClass]) shouldBe result
  }

  it should "not try to deserialize a List" in {
    intercept[DatabindException] {
      deserialize("""{"foo":"foo","bar":"bar"}""", classOf[List[_]])
    }
  }

  it should "deserialize a class with unicode property names" in {
    val result = UnicodeNameCaseClass(23, "the name of this")
    deserialize("""{"winning-id":23,"name":"the name of this"}""", classOf[UnicodeNameCaseClass]) shouldBe result
  }

  it should "deserialize a generic case class" in {
    val result = GenericTestCaseClass(42)
    deserialize("""{"data":42}""", classOf[GenericTestCaseClass[Int]]) shouldBe result
  }

  it should "deserialize Longs properly" in {
    val expected = LongValueCaseClass(1234L, Some(123456789012345678L), Some(5678L))
    val result = deserialize("""{"id":1234,"big":123456789012345678,"small":5678}""", classOf[LongValueCaseClass])

    result shouldBe expected

    result.id.getClass should be (classOf[Long])
    java.lang.Long.valueOf(result.id) should be (1234L)

    result.big.get.getClass should be (classOf[Long])
    result.big.map(java.lang.Long.valueOf) should be (Some(123456789012345678L))

    result.small.get.getClass should be (classOf[Long])
    // this throws a ClassCastException if you comment out the previous line:
    result.small.map(java.lang.Long.valueOf) should be (Some(5678L))
  }

  it should "deserialize Longs in POSOs" in {
    val result = deserialize("""{"small":1}""", classOf[LongValueClass])
    result.small.get shouldBe a[Long]
  }

  def propertyNamingStrategyMapper: ObjectMapper = {
    val builder = JsonMapper.builder()
    val settings = builder.baseSettings().`with`(PropertyNamingStrategies.SNAKE_CASE)
    builder.baseSettings(settings).addModule(module).build()
  }

  it should "honor the property naming strategy" in {
    val result = MixedPropertyNameStyleCaseClass(42, 42, 42, 42, 42)
    propertyNamingStrategyMapper.readValue("""{"camel_case":42,"snake_case":42,"alllower":42,"allupper":42,"an_id":42}""", classOf[MixedPropertyNameStyleCaseClass]) should be (result)
  }

  it should "support serializing into instance var properties" in {
    val bean = new Bean("ctor")
    val reader: ObjectReader = newMapper.readerFor(bean.getClass)
    reader.withValueToUpdate(bean).readValue("""{"prop":"readValue"}""")
    bean.prop should be ("readValue")
  }

  it should "support Array[Byte] properties" in {
    val result = deserialize("""{"value":"AQID"}""", classOf[ArrayHolder])
    result.value should equal (Array[Byte](1,2,3))
  }

  it should "support deserialization with missing field" in {
    val input = """{"hour": "12345"}"""
    val result = deserialize(input, classOf[Time])
    // https://github.com/FasterXML/jackson-module-scala/issues/203
    // this result is not popular with users it has been the behaviour for quite some time
    result shouldEqual Time("12345", null)
  }

  it should "fail deserialization with missing field (DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)" in {
    val mapper = newBuilder.enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES).build()
    val input = """{"hour": "12345"}"""
    intercept[MismatchedInputException] {
      mapper.readValue(input, classOf[Time])
    }
  }

  it should "support ClassWithUnitField" in {
    val input = """{"intField":2}"""
    val result = deserialize(input, classOf[ClassWithUnitField])
    result shouldEqual ClassWithUnitField((), 2)
  }

  //this does not currently work
  it should "support ClassWithOnlyUnitField" ignore {
    val input = """{}"""
    val result = deserialize(input, classOf[ClassWithOnlyUnitField])
    result shouldEqual ClassWithOnlyUnitField(())
  }

  it should "support deserializing null input for list as empty list" in {
    val input = """{}"""
    val result = deserialize(input, classOf[ListHolder[String]])
    // result.list used to be null until v2.19.0
    result.list shouldBe List.empty
  }

  it should "fail when deserializing null input for list if FAIL_ON_NULL_CREATOR_PROPERTIES enabled" in {
    val input = """{}"""
    val mapper = newBuilder.enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES).build()
    intercept[tools.jackson.databind.exc.MismatchedInputException] {
      mapper.readValue(input, classOf[ListHolder[String]])
    }
  }

  it should "support deserializing null input for list as empty list (JsonSetter annotation)" in {
    val input = """{}"""
    val result = deserialize(input, classOf[AnnotatedListHolder[String]])
    result.list shouldBe List.empty
  }

  it should "support deserializing null input for Option[List] as None" in {
    val input = """{}"""
    val result = deserialize(input, classOf[OptionListHolder[String]])
    result.list shouldBe None
  }

  it should "support deserializing empty input for Option[List] as Some(List.empty)" in {
    val input = """{"list":[]}"""
    val result = deserialize(input, classOf[OptionListHolder[String]])
    result.list shouldBe Some(List.empty)
  }

  it should "support deserializing null input for map as empty map" in {
    val input = """{}"""
    val result = deserialize(input, classOf[MapHolder[Int, String]])
    // result.map used to be null until v2.19.0
    result.map shouldBe Map.empty
  }

  it should "support deserializing null input for map as empty list (JsonSetter annotation)" in {
    val input = """{}"""
    val result = deserialize(input, classOf[AnnotatedMapHolder[Int, String]])
    result.map shouldBe Map.empty
  }

  it should "fail when deserializing null input for map if FAIL_ON_NULL_CREATOR_PROPERTIES enabled" in {
    val input = """{}"""
    val mapper = newBuilder.enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES).build()
    intercept[tools.jackson.databind.exc.MismatchedInputException] {
      mapper.readValue(input, classOf[MapHolder[Int, String]])
    }
  }
}
