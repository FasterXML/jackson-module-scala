package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.IntMapDeserializerTest.{Event, IntMapWrapper}

import java.util.UUID
import scala.collection.immutable.IntMap

object IntMapDeserializerTest {
  case class IntMapWrapper(values: IntMap[Long])

  case class Event(id: UUID, description: String)
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

  it should "deserialize IntMap (Object values)" in {
    val event = Event(UUID.randomUUID(), "event1")
    val map = IntMap(0 -> false, 1 -> "true", 2 -> event)
    val mapper = newMapper
    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[IntMap[Any]])
    read(0) shouldBe false
    read(1) shouldEqual "true"
    read(2) shouldEqual Map("id" -> event.id.toString, "description" -> event.description)
  }

  it should "deserialize IntMap[_]" in {
    val event = Event(UUID.randomUUID(), "event1")
    val map = IntMap(0 -> false, 1 -> "true", 2 -> event)
    val mapper = newMapper
    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[IntMap[_]]{})
    read(0) shouldBe false
    read(1) shouldEqual "true"
    read(2) shouldEqual Map("id" -> event.id.toString, "description" -> event.description)
  }

  it should "deserialize IntMap (Object values, duplicate keys - default mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val read = mapper.readValue(json, classOf[IntMap[Any]])
    read(1) shouldEqual 123
    read(2) shouldEqual 123.456
  }

  it should "deserialize IntMap (Object values, duplicate keys - optional mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, classOf[IntMap[Any]])
    } finally {
      parser.close()
    }
    read(1) shouldEqual 123
    val expected = new java.util.ArrayList[Object]()
    expected.add(java.lang.Integer.valueOf(123))
    expected.add(java.lang.Double.valueOf(123.456))
    read(2) shouldEqual expected
  }

  it should "deserialize IntMap (Double values, duplicate key mode is ignored)" in {
    val mapper = newMapper
    val json = """{"1": "123", "2": "123", "2": "123.456"}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, new TypeReference[IntMap[String]]{})
    } finally {
      parser.close()
    }
    read(1) shouldEqual "123"
    read(2) shouldEqual "123.456"
  }
}
