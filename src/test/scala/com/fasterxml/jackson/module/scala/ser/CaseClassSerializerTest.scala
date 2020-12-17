package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonProperty.Access
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.beans.BeanProperty

case class ConstructorTestCaseClass(intValue: Int, stringValue: String)

case class ValTestCaseClass() {
  val intVal: Int = 1
  val strVal: String = "foo"
}

case class VarTestCaseClass() {
  var intVar: Int = 1
  var strVar: String = "foo"
}

case class MixedTestCaseClass(intValue: Int) {
  val strVal: String = "foo"
}

case class JacksonAnnotationTestCaseClass(@JsonProperty("foo") oof:String, bar: String)

case class GenericTestCaseClass[T](data: T)

case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

object CaseClassWithCompanion

case class CaseClassWithCompanion(intValue: Int)

@JsonIgnoreProperties(Array("ignore"))
case class JacksonIgnorePropertyTestCaseClass(ignore:String, test:String)

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="class")
case class JsonTypeInfoCaseClass(intValue: Int)

case class CaseClassContainingJsonTypeInfoCaseClass(c: JsonTypeInfoCaseClass)

case class NonNullCaseClass1(@JsonInclude(JsonInclude.Include.NON_NULL) foo: String)

case class NonNullCaseClass2(foo: String)

case class MixedPropertyNameStyleCaseClass(camelCase: Int, snake_case: Int, alllower: Int, ALLUPPER: Int, anID: Int)

class NonCaseWithBeanProperty {
  @BeanProperty var id: Int = _
  @BeanProperty var bar: String = _
}

case class InnerJavaEnum(fieldType: Field.Type)

case class PrivateDefaultFields(
  @JsonProperty(access = Access.READ_ONLY) private val firstName: String,
  @JsonProperty lastName: String = "Freeman"
)

case class LazyClass(data: String) {
  lazy val lazyString: String = data
  @JsonIgnore
  lazy val lazyIgnoredString: String = data
}

@RunWith(classOf[JUnitRunner])
class CaseClassSerializerTest extends SerializerTest {

  case class NestedClass(field: String)

  def module = DefaultScalaModule

  "An ObjectMapper with the CaseClassModule" should "serialize a case class as a bean" in {
    serialize(ConstructorTestCaseClass(1,"A")) should (
       equal ("""{"intValue":1,"stringValue":"A"}""") or
       equal ("""{"stringValue":"A","intValue":1}""")
    )
  }

  it should "serialize a class class with val members" in {
    serialize(ValTestCaseClass()) should (
      equal ("""{"intVal":1,"strVal":"foo"}""") or
      equal ("""{"strVal":"foo","intVal":1}""")
    )
  }

  it should "serialize a class class with var members" in {
    serialize(VarTestCaseClass()) should (
      equal ("""{"intVar":1,"strVar":"foo"}""") or
      equal ("""{"strVar":"foo","intVar":1}""")
    )
  }

  it should "serialize a class class with both constructor and member properties" in {
    serialize(MixedTestCaseClass(99)) should (
      equal ("""{"intValue":99,"strVal":"foo"}""") or
      equal ("""{"strVal":"foo","intValue":99}""")
    )
  }

  it should "honor Jackson annotations" in {
    serialize(JacksonAnnotationTestCaseClass("foo","bar")) should equal( """{"foo":"foo","bar":"bar"}""")
  }

  it should "serialize a case class with ignore property annotations" in {
    serialize(JacksonIgnorePropertyTestCaseClass("ignore", "test")) should equal( """{"test":"test"}""")
  }

  it should "serialize a case class with unicode name properties" in {
    serialize(UnicodeNameCaseClass(23, "the name of this")) should (
      equal( """{"name":"the name of this","winning-id":23}""") or
      equal( """{"winning-id":23,"name":"the name of this"}""")
    )
  }

  it should "seralize a generic case class" in {
    serialize(GenericTestCaseClass(42)) should equal( """{"data":42}""")
  }

  it should "serialize a case class with a companion object" in {
    serialize(CaseClassWithCompanion(42)) should equal( """{"intValue":42}""")
  }

  def nonNullMapper: ObjectMapper =
    new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .registerModule(DefaultScalaModule)

  it should "not write a null value" in {
    val o = NonNullCaseClass1(null)
    nonNullMapper.writeValueAsString(o) should be ("{}")
  }

  it should "not also write a null value" in {
    val o = NonNullCaseClass2(null)
    nonNullMapper.writeValueAsString(o) should be ("{}")
  }

  def propertyNamingStrategyMapper = new ObjectMapper() {
    registerModule(module)
    setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
  }

  it should "honor the property naming strategy" in {
    val o = MixedPropertyNameStyleCaseClass(42, 42, 42, 42, 42)
    propertyNamingStrategyMapper.writeValueAsString(o) should be("""{"camel_case":42,"snake_case":42,"alllower":42,"allupper":42,"an_id":42}""")
  }

  it should "serialize a case class with JsonTypeInfo" in {
    serialize(JsonTypeInfoCaseClass(1)) should equal( """{"class":"com.fasterxml.jackson.module.scala.ser.JsonTypeInfoCaseClass","intValue":1}""")
  }

  it should "serialize a case class containing a case class with JsonTypeInfo" in {
    serialize(CaseClassContainingJsonTypeInfoCaseClass(JsonTypeInfoCaseClass(1))) should equal( """{"c":{"class":"com.fasterxml.jackson.module.scala.ser.JsonTypeInfoCaseClass","intValue":1}}""")
  }

  it should "serialize a non-case class with @BeanProperty annotations" in {
    val bean = new NonCaseWithBeanProperty
    bean.id = 1
    bean.bar = "foo"
    serialize(bean) should equal( """{"id":1,"bar":"foo"}""")
  }

  it should "serialize a nested case class" in {
    val bean = NestedClass("nested")
    serialize(bean) should equal( """{"field":"nested"}""")
  }

  it should "serialize a case class containing an inner Java enum" in {
    val result = serialize(InnerJavaEnum(Field.Type.TYPEA))
    result should be ("""{"fieldType":"TYPEA"}""")
  }

  it should "serialize private fields annotated with @JsonProperty" in {
    val result = serialize(PrivateDefaultFields("Gordon", "Biersch"))
    result should be ("""{"firstName":"Gordon","lastName":"Biersch"}""")
  }

  it should "serialize java getters" in {
    class Foo(string: String, boolean: Boolean) {
      def getString = string
      def isBoolean = boolean
    }
    val foo = new Foo("str", false)
    serialize(foo) should equal ("""{"string":"str","boolean":false}""")
  }

  it should "serialize java getters returning java collections" in {
    class Foo(strings: java.util.List[String]) {
      def getStrings: java.util.List[String] = strings
    }
    val foo = new Foo(java.util.Arrays.asList("foo", "bar"))
    serialize(foo) should equal ("""{"strings":["foo","bar"]}""")
  }

  it should "exclude bitmap$0 field from serialization" in {
    val lazyInstance = LazyClass("test")
    serialize(lazyInstance) should equal ("""{"data":"test","lazyString":"test"}""")
  }
}
