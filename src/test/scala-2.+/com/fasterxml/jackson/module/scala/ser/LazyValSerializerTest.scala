package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.DefaultScalaModule

class LazyValSerializerTest extends SerializerTest {

  def module = DefaultScalaModule

  "LazyValSerializer" should "exclude bitmap$0 field from serialization" in {
    val lazyInstance = LazyClass("test")
    serialize(lazyInstance) should equal ("""{"data":"test","lazyString":"test"}""")
  }
}
