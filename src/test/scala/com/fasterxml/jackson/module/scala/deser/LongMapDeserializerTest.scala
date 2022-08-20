package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.{immutable, mutable}

class LongMapDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "Scala Module" should "deserialize immutable LongMap" in {
    val map = immutable.LongMap(1L -> "one", 2L -> "two")

    val mapper = newBuilder.addModule(new LongMapDeserializerModule() {}).build()

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[immutable.LongMap[String]] {})

    read shouldEqual map
  }

  it should "deserialize mutable LongMap" in {
    val map = mutable.LongMap(1L -> "one", 2L -> "two")

    val mapper = newBuilder.addModule(new LongMapDeserializerModule() {}).build()

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[mutable.LongMap[String]] {})

    read shouldEqual map
  }
}
