package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.JsonPropertyDeserializerTest

class JsonPropertySerializerTest extends SerializerTest {
  import JsonPropertyDeserializerTest._
  def module: DefaultScalaModule.type = DefaultScalaModule

  "ObjectMapper serialization" should "support JsonProperty annotation" in {
    val json = """{"v":1}"""
    val sample1 = new Sample1
    sample1.setValue(1)
    serialize(sample1) shouldEqual json
    val sample1a = new Sample1a
    sample1a.value = 1
    serialize(sample1a) shouldEqual json
    val sample2 = new Sample2
    sample2.setValue(1)
    serialize(sample2) shouldEqual json
  }
}
