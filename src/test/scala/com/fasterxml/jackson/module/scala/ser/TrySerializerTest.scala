package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class TrySerializerTest extends SerializerTest {

  val module: JacksonModule = DefaultScalaModule

  val str = "value"
  val json: JsonNode = jsonOf(s"""{"prop":"$str"}""")

  "TrySerializer" should "be able to serialize success with string" in {
    serialize(Success(str)) should be (s"""{"value":"$str","failure":false,"success":true}""")
  }
}
