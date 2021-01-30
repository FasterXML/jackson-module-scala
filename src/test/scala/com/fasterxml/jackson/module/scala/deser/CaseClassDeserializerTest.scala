package com.fasterxml.jackson
package module.scala
package deser

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper, ObjectReader, PropertyNamingStrategy}

import java.time.LocalDateTime

object CaseClassDeserializerTest
{
  class Bean(var prop: String)

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

  class LongValueClass
  {
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
}

class CaseClassDeserializerTest extends DeserializerTest {
  import com.fasterxml.jackson.module.scala.deser.CaseClassDeserializerTest._

  def module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with CaseClassDeserializer" should "deserialize a case class with a single constructor" in {
    deserialize("""{"intValue":1,"stringValue":"foo"}""", classOf[ConstructorTestCaseClass]) should be (ConstructorTestCaseClass(1,"foo"))
  }

  it should "deserialize a case class with multiple constructors" in {
    val json = """{"path":{"path":"/path","level":1,"isRoot":false},"value":0.5,"time":"2017-05-10T00:00:00.000+02:00","tags":[]}"""
    deserialize(json, classOf[Metric]) shouldBe Metric(MetricPath("/path", 1, false), 0.5, "2017-05-10T00:00:00.000+02:00")
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
    intercept[JsonMappingException] {
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

  def propertyNamingStrategyMapper: ObjectMapper = new ObjectMapper() {
    registerModule(module)
    setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
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
}
