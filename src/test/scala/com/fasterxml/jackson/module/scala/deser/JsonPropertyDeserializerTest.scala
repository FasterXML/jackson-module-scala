package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

//https://github.com/FasterXML/jackson-module-scala/issues/224
object JsonPropertyDeserializerTest {
  //try multiple variants because some variants have caused more issues than others
  class Sample1 {
    @JsonProperty("v") private var value: Int = 0

    def getValue: Int = value
    def setValue(value: Int): Unit  = {this.value = value}
  }

  class Sample1a {
    @JsonProperty("v") var value: Int = 0
  }

  class Sample2 {
    private var value: Int = 0

    @JsonProperty("v") def getValue: Int = value
    @JsonProperty("v") def setValue(value: Int): Unit  = {this.value = value}
  }
}

class JsonPropertyDeserializerTest extends DeserializerTest {
  import JsonPropertyDeserializerTest._
  def module: DefaultScalaModule.type = DefaultScalaModule

  "ObjectMapper deserialization" should "support JsonProperty annotation" in {
    val json = """{"v": 1}"""
    val token1 = deserialize(json, classOf[Sample1])
    token1.getValue shouldEqual 1
    val token1a = deserialize(json, classOf[Sample1a])
    token1a.value shouldEqual 1
    val token2 = deserialize(json, classOf[Sample2])
    token2.getValue shouldEqual 1
  }
  it should "support JsonProperty annotation (without scala module)" in {
    val mapper = JsonMapper.builder().build()
    val json = """{"v": 1}"""
    val token1 = mapper.readValue(json, classOf[Sample1])
    token1.getValue shouldEqual 1
    val token1a = mapper.readValue(json, classOf[Sample1a])
    token1a.value shouldEqual 1
    val token2 = mapper.readValue(json, classOf[Sample2])
    token2.getValue shouldEqual 1
  }
}
