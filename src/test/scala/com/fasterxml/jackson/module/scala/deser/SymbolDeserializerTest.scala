package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SymbolDeserializerTest extends DeserializerTest {
  lazy val module = new JacksonModule with SymbolDeserializerModule

  "An ObjectMapper with the SymbolDeserializer" should "deserialize a string into a Symbol" in {
    val result = deserialize[Symbol](""""symbol"""")
    result should equal (Symbol("symbol"))
  }
}
