package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.DefaultScalaModule

case object CaseObjectExample {
  val field1: String = "test"
  val field2: Int = 42
}

class CaseObjectSerializerTest extends SerializerTest {

  def module = DefaultScalaModule

  "An ObjectMapper with the DefaultScalaModule" should "serialize a case object as a bean" in {
    serialize(CaseObjectExample) should (
       equal ("""{"field1":"test","field2":42}""") or
       equal ("""{"field2":42,"field1":"test"}""")
    )
  }
}
