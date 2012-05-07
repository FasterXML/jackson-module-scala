package com.fasterxml.jackson.module.scala.ser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers



import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}

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

object CaseClassWithCompanion {
}

case class CaseClassWithCompanion(intValue: Int)

@JsonIgnoreProperties(Array("ignore"))
case class JacksonIgnorePropertyTestCaseClass(ignore:String, test:String)

@RunWith(classOf[JUnitRunner])
class CaseClassSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  def module = new JacksonModule with CaseClassSerializerModule {}

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
    serialize(JacksonAnnotationTestCaseClass("foo","bar")) should (
      equal("""{"foo":"foo","bar":"bar"}""")
      )
  }

  it should "serialize a case class with ignore property annotations" in {
    serialize(JacksonIgnorePropertyTestCaseClass("ignore", "test")) should (
      equal("""{"test":"test"}""")
      )
  }

  it should "seralize a generic case class" in {
    serialize(GenericTestCaseClass(42)) should (
      equal("""{"data":42}""")
      )
  }

  it should "serialize a case class with a companion object" in {
    serialize(CaseClassWithCompanion(42)) should (
      equal("""{"intValue":42}""")
      )
  }
}