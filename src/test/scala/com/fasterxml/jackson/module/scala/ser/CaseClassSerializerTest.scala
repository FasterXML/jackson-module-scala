package com.fasterxml.jackson.module.scala.ser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.annotate.{JsonIgnoreProperties, JsonProperty}

case class CaseClassConstructorTest(intValue: Int, stringValue: String)

case class CaseClassValTest() {
  val intVal: Int = 1
  val strVal: String = "foo"
}

case class CaseClassVarTest() {
  var intVar: Int = 1
  var strVar: String = "foo"
}

case class CaseClassMixedTest(intValue: Int) {
  val strVal: String = "foo"
}

case class CaseClassJacksonAnnotationTest(@JsonProperty("foo") oof:String, bar: String)

case class GenericCaseClassTest[T](data: T)

object CaseClassWithCompanion {
}

case class CaseClassWithCompanion(intValue: Int)

@JsonIgnoreProperties(Array("ignore"))
case class CaseClassJacksonIgnorePropertyTest(ignore:String, test:String)

@RunWith(classOf[JUnitRunner])
class CaseClassSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  def module = new JacksonModule with CaseClassSerializerModule {}

  "An ObjectMapper with the CaseClassModule" should "serialize a case class as a bean" in {
    serialize(CaseClassConstructorTest(1,"A")) should (
       equal ("""{"intValue":1,"stringValue":"A"}""") or
       equal ("""{"stringValue":"A","intValue":1}""")
    )
  }

  it should "serialize a class class with val members" in {
    serialize(CaseClassValTest()) should (
      equal ("""{"intVal":1,"strVal":"foo"}""") or
      equal ("""{"strVal":"foo","intVal":1}""")
    )
  }

  it should "serialize a class class with var members" in {
    serialize(CaseClassVarTest()) should (
      equal ("""{"intVar":1,"strVar":"foo"}""") or
      equal ("""{"strVar":"foo","intVar":1}""")
    )
  }

  it should "serialize a class class with both constructor and member properties" in {
    serialize(CaseClassMixedTest(99)) should (
      equal ("""{"intValue":99,"strVal":"foo"}""") or
      equal ("""{"strVal":"foo","intValue":99}""")
    )
  }

  it should "honor Jackson annotations" in {
    serialize(CaseClassJacksonAnnotationTest("foo","bar")) should (
      equal("""{"foo":"foo","bar":"bar"}""")
      )
  }

  it should "serialize a case class with ignore property annotations" in {
    serialize(CaseClassJacksonIgnorePropertyTest("ignore", "test")) should (
      equal("""{"test":"test"}""")
      )
  }

  it should "seralize a generic case class" in {
    serialize(GenericCaseClassTest(42)) should (
      equal("""{"data":42}""")
      )
  }

  it should "serialize a case class with a companion object" in {
    serialize(CaseClassWithCompanion(42)) should (
      equal("""{"intValue":42}""")
      )
  }
}