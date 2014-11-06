package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.JacksonTest

/**
 * Undocumented class.
 */

trait SerializerTest extends JacksonTest {

  def serialize(value: Any): String = mapper.writeValueAsString(value)
  
  def jsonOf(s: String): JsonNode = mapper.readTree(s)

}