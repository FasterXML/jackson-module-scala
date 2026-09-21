package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSetter, Nulls}
import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{DatabindException, DeserializationFeature, ObjectMapper, ObjectReader, PropertyNamingStrategies}
import tools.jackson.module.scala.{DefaultScalaModule, ScalaModule}
import tools.jackson.module.scala.ser.{ClassWithOnlyUnitField, ClassWithUnitField}

import java.time.LocalDateTime

object CaseClassDeserializerTest {
  class Bean(var prop: String)

  case class Node(value: Int, next: Option[Node])

  // one parameter of every primitive kind, plus the reference kinds a missing property leaves null
  case class MissingValues(byte: Byte, short: Short, int: Int, long: Long, float: Float, double: Double,
                           boolean: Boolean, char: Char, boxed: java.lang.Integer, big: BigInt, decimal: BigDecimal,
                           text: String, child: Node, @JsonIgnore ignored: Int, explicit: Int = 9)

  // past the 22-parameter limit of Product/Function; only the last parameter has a default
  case class Wide(f1: Int, f2: Int, f3: Int, f4: Int, f5: Int, f6: Int, f7: Int, f8: Int, f9: Int, f10: Int,
                  f11: Int, f12: Int, f13: Int, f14: Int, f15: Int, f16: Int, f17: Int, f18: Int, f19: Int,
                  f20: Int, f21: Int, f22: Int, f23: Int, f24: Int = 24)

  trait NamedValue {
    val name: String
  }
  case class InheritedValue(name: String) extends NamedValue

  // a secondary constructor and a companion apply that both take a single `value`
  case class AmbiguousConstructor(value: Int) {
    def this(value: String) = this(value.toInt)
  }
  object AmbiguousConstructor {
    def apply(value: String): AmbiguousConstructor = new AmbiguousConstructor(value)
  }

  case class Box[T](value: T)

  case class Time(hour: String, minute: String)

  case class ConstructorTestCaseClass(intValue: Int, stringValue: String)

  case class PropertiesTestCaseClass() {
    var intProperty: Int = 0
    var stringProperty: String = _
  }

  case class JacksonAnnotationTestCaseClass(@JsonProperty("foo") oof: String, bar: String)

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

  case class VarTestConstructor(var test: Int)
  case class AnnotatedVarTestConstructor(@JsonProperty("t") var test: Int)

  case class SecurityProfile(snoozeAlerts: Boolean, id: Int, autoverifyAlerts: Boolean)
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

  it should "deserialize list as null if deserializeNullCollectionsAsEmpty config is false" in {
    val input = """{}"""
    val module = ScalaModule.builder()
      .deserializeNullCollectionsAsEmpty(false)
      .addAllBuiltinModules()
      .build()
    val mapper = JsonMapper.builder().addModule(module).build()
    val res = mapper.readValue(input, classOf[ListHolder[String]])
    res.list shouldBe null
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

  it should "deserialize map as null if deserializeNullCollectionsAsEmpty config is false" in {
    val input = """{}"""
    val module = ScalaModule.builder()
      .deserializeNullCollectionsAsEmpty(false)
      .addAllBuiltinModules()
      .build()
    val mapper = JsonMapper.builder().addModule(module).build()
    val res = mapper.readValue(input, classOf[MapHolder[Int, String]])
    res.map shouldBe null
  }

  it should "deserialize VarTestConstructor" in {
    val input = """{"test":123}"""
    val res = newMapper.readValue(input, classOf[VarTestConstructor])
    res.test shouldEqual 123
  }

  it should "deserialize AnnotatedVarTestConstructor" in {
    val input = """{"t":123}"""
    val res = newMapper.readValue(input, classOf[AnnotatedVarTestConstructor])
    res.test shouldEqual 123
  }

  // https://github.com/FasterXML/jackson-module-scala/issues/762
  it should "deserialize SecurityProfile" in {
    val input = """{"id": 1069, "snoozeAlerts": true, "autoverifyAlerts": false}"""
    val builder = newBuilder
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val settings = builder.baseSettings().`with`(PropertyNamingStrategies.LOWER_CAMEL_CASE)
    val mapper = builder.baseSettings(settings).build
    mapper.readValue(input, classOf[SecurityProfile]) shouldEqual SecurityProfile(true, 1069, false)
  }

  it should "use the type default for every kind of missing constructor parameter" in {
    val expected = MissingValues(0, 0, 0, 0L, 0f, 0d, false, 0.toChar, null, null, null, null, null, 0, 9)
    deserialize("{}", classOf[MissingValues]) shouldEqual expected
    val lenient = newBuilder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
    lenient.readValue("""{"unknown":"中"}""", classOf[MissingValues]) shouldEqual expected
  }

  it should "deserialize a recursive case class with missing fields" in {
    deserialize("{}", classOf[Node]) shouldEqual Node(0, None)
    deserialize("""{"value":1}""", classOf[Node]) shouldEqual Node(1, None)
    deserialize("""{"value":1,"next":{"value":2,"next":null}}""", classOf[Node]) shouldEqual Node(1, Some(Node(2, None)))
  }

  it should "deserialize a case class with more than 22 parameters" in {
    val wide = Wide(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
    val json = serialize(wide)
    json should include(""""f24":24""")
    deserialize(json, classOf[Wide]) shouldEqual wide
    deserialize(json.replace(""","f24":24""", ""), classOf[Wide]) shouldEqual wide
    val sparse = deserialize("""{"f1":1}""", classOf[Wide])
    sparse.f1 shouldEqual 1
    sparse.f2 shouldEqual 0
    sparse.f23 shouldEqual 0
    sparse.f24 shouldEqual 24
  }

  it should "deserialize a case class that implements a trait val" in {
    serialize(InheritedValue("中")) shouldEqual """{"name":"中"}"""
    deserialize("""{"name":"中"}""", classOf[InheritedValue]) shouldEqual InheritedValue("中")
  }

  it should "use the primary constructor when a secondary constructor and a companion apply share its parameter name" in {
    deserialize("""{"value":1}""", classOf[AmbiguousConstructor]) shouldEqual AmbiguousConstructor(1)
  }

  it should "deserialize nested generic case classes" in {
    val nested = new TypeReference[Box[Box[Option[Int]]]] {}
    deserialize("""{"value":{"value":1}}""", nested) shouldEqual Box(Box(Some(1)))
    deserialize("""{"value":{"value":null}}""", nested) shouldEqual Box(Box(None))
    serialize(Box(Box(Some(1): Option[Int]))) shouldEqual """{"value":{"value":1}}"""
  }
}
