package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

class ArraySeqSerializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with the SeqDeserializer" should "handle ArraySeq of booleans" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val size = 1000
    val obj = (1 to size).map(i => ((i * 1498724053) & 0x1) == 0).toArray
    val jsonString = obj.mkString("[", ",", "]")
    val jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8)
    val seq = mapper.readValue(jsonBytes, new TypeReference[ArraySeq[Boolean]] {})
    seq should have size size
  }
}
