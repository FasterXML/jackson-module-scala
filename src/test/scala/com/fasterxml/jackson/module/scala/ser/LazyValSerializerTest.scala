package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object LazyValSerializerTest {
  case class LazyClass(data: String) {
    lazy val lazyString: String = data
    @JsonIgnore
    lazy val lazyIgnoredString: String = data
  }
}

class LazyValSerializerTest extends SerializerTest {

  def module = DefaultScalaModule

  "LazyValSerializer" should "exclude bitmap$0 field from serialization" in {
    val lazyInstance = LazyValSerializerTest.LazyClass("test")
    serialize(lazyInstance) should equal ("""{"data":"test","lazyString":"test"}""")
  }
}
