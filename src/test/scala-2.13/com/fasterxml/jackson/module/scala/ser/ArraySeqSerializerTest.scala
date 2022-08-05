package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.collection.immutable
import scala.collection.mutable

class ArraySeqSerializerTest extends SerializerTest {

  lazy val module: JacksonModule = DefaultScalaModule
  val arraySize = 50
  val array = (1 to arraySize).map(i => ((i * 1498724053) & 0x1) == 0).toArray

  "An ObjectMapper with the SeqSerializer" should "handle immutable ArraySeq of booleans" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val testSeq = immutable.ArraySeq.from(array)
    val jsonString = mapper.writeValueAsString(testSeq)
    val seq = mapper.readValue(jsonString, new TypeReference[immutable.ArraySeq[Boolean]] {})
    seq shouldEqual testSeq
  }

  it should "handle mutable ArraySeq of booleans" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val testSeq = mutable.ArraySeq.from(array)
    val jsonString = mapper.writeValueAsString(testSeq)
    val seq = mapper.readValue(jsonString, new TypeReference[mutable.ArraySeq[Boolean]] {})
    seq shouldEqual testSeq
  }
}
