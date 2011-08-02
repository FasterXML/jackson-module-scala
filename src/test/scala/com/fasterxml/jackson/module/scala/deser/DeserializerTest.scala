package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.TypeReference
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.{ScalaModule, JacksonTest}

/**
 * Undocumented class.
 */

trait DeserializerTest extends JacksonTest {

  // Not much of a helper function but it exists for parity with SerializerTest
  def deserialize[T](value: String, valueType: TypeReference[T]) : T = {
    mapper.readValue(value, valueType)
  }


}