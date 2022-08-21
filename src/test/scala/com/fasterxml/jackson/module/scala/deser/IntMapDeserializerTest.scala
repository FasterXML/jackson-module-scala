package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.IntMapDeserializerTest.IntMapWrapper

import scala.collection.immutable.IntMap

object IntMapDeserializerTest {
  case class IntMapWrapper(values: IntMap[Long])
}

class IntMapDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "Scala Module" should "deserialize IntMap" in {
    val map = IntMap(1 -> "one", 2 -> "two")

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[String]]{})

    read shouldBe map
  }

  it should "deserialize IntMap (long value)" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[Long]] {})

    read shouldEqual map
    // next line fails due to type erasure (values are ints and won't cast to longs)
    //read.values.sum shouldEqual map.values.sum
  }

  it should "deserialize IntMapWrapper" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)
    val instance = IntMapWrapper(map)

    val mapper = newMapper

    val json = mapper.writeValueAsString(instance)
    val read = mapper.readValue(json, classOf[IntMapWrapper])

    read shouldEqual instance
    // next line fails due to type erasure (values are ints and won't cast to longs)
    //read.values.values.sum shouldEqual map.values.sum
  }

  it should "deserialize IntMap (bigint value)" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[BigInt]] {})

    read shouldEqual map
    read.values.sum shouldEqual map.values.sum
  }

  it should "deserialize IntMap (boolean value)" in {
    val map = IntMap(0 -> false, 402 -> true)
    val mapper = newMapper
    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[IntMap[Boolean]])
    read shouldEqual map
    read(0) shouldBe false
    read(402) shouldBe true
  }
}
