package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.immutable.IntMap

class IntMapDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "Scala Module" should "deserialize IntMap" in {
    val map = IntMap(1 -> "one", 2 -> "two")

    val mapper = newBuilder.addModule(new IntMapDeserializerModule() {}).build()

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[String]]{})

    read shouldBe map
  }

  it should "deserialize IntMap (long value)" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)

    val mapper = newBuilder.addModule(new IntMapDeserializerModule() {}).build()

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[Long]] {})

    read shouldEqual map
    read.values.sum shouldEqual map.values.sum
  }
}
