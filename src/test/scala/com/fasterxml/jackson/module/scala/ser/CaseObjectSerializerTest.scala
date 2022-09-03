package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.{JsonAutoDetect, PropertyAccessor}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case object CaseObjectExample {
  val field1: String = "test"
  val field2: Int = 42
}

class CaseObjectSerializerTest extends SerializerTest {

  case object Foo {
    val field: String = "bar"
  }

  def module = DefaultScalaModule

  "An ObjectMapper with the DefaultScalaModule" should "serialize a case object as a bean" in {
    serialize(CaseObjectExample) should (
       equal ("""{"field1":"test","field2":42}""") or
         equal ("""{"field2":42,"field1":"test"}""")
    )
  }

  // https://github.com/FasterXML/jackson-module-scala/issues/596
  it should "serialize a case object when visibility settings set" ignore {
    val mapper = newBuilder
      .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
      .build()
    mapper.writeValueAsString(CaseObjectExample) should (
      equal("""{"field1":"test","field2":42}""") or
        equal("""{"field2":42,"field1":"test"}""")
    )
  }

  it should "serialize an inner case object when visibility settings set" in {
    val mapper = newBuilder
      .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
      .build()
    mapper.writeValueAsString(Foo) shouldEqual """{"field":"bar"}"""
  }
}
