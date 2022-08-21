package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.module.scala.DefaultScalaModule

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
    val read = mapper.readValue(json, classOf[immutable.LongMap[Boolean]])

    read shouldEqual map
    read(0) shouldBe false
    read(402) shouldBe true
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
}
