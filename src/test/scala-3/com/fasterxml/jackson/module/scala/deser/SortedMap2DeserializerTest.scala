package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.collection.immutable

class SortedMap2DeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new SortedMapDeserializerModule {}

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into an TreeSeqMap" in {
    val result = deserialize(mapJson, new TypeReference[immutable.TreeSeqMap[String,String]]{})
    result should equal (mapScala)
  }

  private val mapJson =  """{ "one": "1", "two": "2" }"""
  private val mapScala = collection.SortedMap("one"->"1","two"->"2")
}
