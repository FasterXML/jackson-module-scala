package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import java.nio.charset.StandardCharsets
import scala.collection.immutable
import scala.collection.mutable

class ArraySeqDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule
  val arraySize = 1000
  val obj = (1 to arraySize).map(i => ((i * 1498724053) & 0x1) == 0).toArray
  val jsonString = obj.mkString("[", ",", "]")
  val jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8)

  "An ObjectMapper with the SeqDeserializer" should "handle immutable ArraySeq of booleans" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val seq = mapper.readValue(jsonBytes, new TypeReference[immutable.ArraySeq[Boolean]] {})
    seq should have size arraySize
  }

  it should "handle mutable ArraySeq of booleans" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val seq = mapper.readValue(jsonBytes, new TypeReference[mutable.ArraySeq[Boolean]] {})
    seq should have size arraySize
  }
}
