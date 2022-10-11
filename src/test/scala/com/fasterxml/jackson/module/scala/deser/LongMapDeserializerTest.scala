package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.IntMapDeserializerTest.Event

import java.util.UUID
import scala.collection.{immutable, mutable}

class LongMapDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "Scala Module" should "deserialize immutable LongMap" in {
    val map = immutable.LongMap(1L -> "one", 2L -> "two")

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[immutable.LongMap[String]] {})

    read shouldEqual map
  }

  it should "deserialize immutable LongMap (bigint)" in {
    val map = immutable.LongMap(1L -> 100, 2L -> 200)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[immutable.LongMap[BigInt]] {})

    read shouldEqual map
    read.values.sum shouldEqual map.values.sum
  }

  it should "deserialize immutable LongMap (boolean)" in {
    val map = immutable.LongMap(0L -> false, 402L -> true)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[mutable.LongMap[Boolean]])

    read shouldEqual map
    read(0) shouldBe false
    read(402) shouldBe true
  }

  it should "deserialize immutable LongMap (Object values)" in {
    val event = Event(UUID.randomUUID(), "event1")
    val map = mutable.LongMap(0L -> false, 1L -> "true", 2L -> event)
    val mapper = newMapper
    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[immutable.LongMap[Any]])
    read(0) shouldBe false
    read(1) shouldEqual "true"
    read(2) shouldEqual Map("id" -> event.id.toString, "description" -> event.description)
  }

  it should "deserialize immutable LongMap (Object values, duplicate keys - default mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val read = mapper.readValue(json, classOf[immutable.LongMap[Any]])
    read(1) shouldEqual 123
    read(2) shouldEqual 123.456
  }

  it should "deserialize immutable LongMap (Object values, duplicate keys - optional mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, classOf[immutable.LongMap[Any]])
    } finally {
      parser.close()
    }
    read(1) shouldEqual 123
    val expected = new java.util.ArrayList[Object]()
    expected.add(java.lang.Integer.valueOf(123))
    expected.add(java.lang.Double.valueOf(123.456))
    read(2) shouldEqual expected
  }

  it should "deserialize immutable LongMap (Double values, duplicate key mode is ignored)" in {
    val mapper = newMapper
    val json = """{"1": "123", "2": "123", "2": "123.456"}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, new TypeReference[immutable.LongMap[String]] {})
    } finally {
      parser.close()
    }
    read(1) shouldEqual "123"
    read(2) shouldEqual "123.456"
  }

  it should "deserialize mutable LongMap" in {
    val map = mutable.LongMap(1L -> "one", 2L -> "two")

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[mutable.LongMap[String]] {})

    read shouldEqual map
  }

  it should "deserialize mutable LongMap (bigint)" in {
    val map = mutable.LongMap(1L -> 100, 2L -> 200)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, new TypeReference[mutable.LongMap[BigInt]] {})

    read shouldEqual map
    read.values.sum shouldEqual map.values.sum
  }

  it should "deserialize mutable LongMap (boolean)" in {
    val map = mutable.LongMap(0L -> false, 402L -> true)

    val mapper = newMapper

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[mutable.LongMap[Boolean]])

    read shouldEqual map
    read(0) shouldBe false
    read(402) shouldBe true
  }

  it should "deserialize mutable LongMap (Object values)" in {
    val event = Event(UUID.randomUUID(), "event1")
    val map = mutable.LongMap(0L -> false, 1L -> "true", 2L -> event)
    val mapper = newMapper
    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue(json, classOf[mutable.LongMap[Any]])
    read(0) shouldBe false
    read(1) shouldEqual "true"
    read(2) shouldEqual Map("id" -> event.id.toString, "description" -> event.description)
  }

  it should "deserialize mutable LongMap (Object values, duplicate keys - default mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val read = mapper.readValue(json, classOf[mutable.LongMap[Any]])
    read(1) shouldEqual 123
    read(2) shouldEqual 123.456
  }

  it should "deserialize mutable LongMap (Object values, duplicate keys - optional mode)" in {
    val mapper = newMapper
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, classOf[mutable.LongMap[Any]])
    } finally {
      parser.close()
    }
    read(1) shouldEqual 123
    val expected = new java.util.ArrayList[Object]()
    expected.add(java.lang.Integer.valueOf(123))
    expected.add(java.lang.Double.valueOf(123.456))
    read(2) shouldEqual expected
  }

  it should "deserialize mutable LongMap (Double values, duplicate key mode is ignored)" in {
    val mapper = newMapper
    val json = """{"1": "123", "2": "123", "2": "123.456"}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, new TypeReference[mutable.LongMap[String]] {})
    } finally {
      parser.close()
    }
    read(1) shouldEqual "123"
    read(2) shouldEqual "123.456"
  }
}
