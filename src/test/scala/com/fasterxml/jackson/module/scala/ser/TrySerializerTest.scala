package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.util.Success

class TrySerializerTest extends SerializerTest {

  val module: JacksonModule = DefaultScalaModule

  val str = "value"
  val json: JsonNode = jsonOf(s"""{"prop":"$str"}""")

  "TrySerializer" should "be able to serialize success with string" in {
    val result = serialize(Success(str))
    result should include(s""""value":"$str"""")
    result should include(""""failure":false""")
    result should include(""""success":true""")
  }
}
