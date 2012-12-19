package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonTest

/**
 * Undocumented class.
 */

trait DeserializerTest extends JacksonTest {

  def deserialize[T: Manifest](value: String) : T =
    mapper.readValue[T](value)
}