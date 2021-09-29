package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.JsonPropertyTest.Sample1

//https://github.com/FasterXML/jackson-module-scala/issues/224
object JsonPropertyTest {
  @JsonIgnoreProperties(ignoreUnknown = true)
  class Sample1 {
    @JsonProperty("v") private var value: Int = 0

    def getValue: Int = value
    def setValue(value: Int): Unit  = {this.value = value}
  }
}

class JsonPropertyTest extends DeserializerTest {
  def module: DefaultScalaModule.type = DefaultScalaModule

  //following test passes in jackson 2.13
  "ObjectMapper deserialization" should "support JsonProperty annotation" ignore {
    val json = """{"v": 1}"""
    val token = deserialize(json, classOf[Sample1])
    token.getValue shouldEqual 1
  }
  it should "support JsonProperty annotation (without scala module)" in {
    val mapper = JsonMapper.builder().build()
    val json = """{"v": 1}"""
    val token = mapper.readValue(json, classOf[Sample1])
    token.getValue shouldEqual 1
  }
}
