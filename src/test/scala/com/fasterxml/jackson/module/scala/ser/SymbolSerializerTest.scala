package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.JacksonModule

class SymbolSerializerTest extends SerializerTest {
  lazy val module = new JacksonModule with SymbolSerializerModule

  "An ObjectMapper with the SymbolSerializer" should "serialize a Symbol using its name" in {
    val result = serialize(Symbol("symbol"))
    result should be (""""symbol"""")
  }
}
