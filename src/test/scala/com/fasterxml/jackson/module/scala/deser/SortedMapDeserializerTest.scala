package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.collection.immutable

class SortedMapDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new SortedMapDeserializerModule {}

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into a SortedMap" in {
    val result = deserialize(mapJson, new TypeReference[collection.SortedMap[String,String]]{})
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable SortedMap" in {
    val result = deserialize(mapJson, new TypeReference[immutable.SortedMap[String,String]]{})
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable SortedMap" in {
    import overrides._
    val result = deserialize(mapJson, new TypeReference[SortedMap[String,String]]{})
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable TreeMap" in {
    val result = deserialize(mapJson, new TypeReference[immutable.TreeMap[String,String]]{})
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable TreeMap" in {
    import overrides._
    val result = deserialize(mapJson, new TypeReference[TreeMap[String,String]]{})
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize(variantMapJson, new TypeReference[collection.SortedMap[String,Any]]{})
    result should equal (variantMapScala)
  }

  it should "deserialize an object with numeric keys into a SortedMap" in {
    // NB: This is `java.lang.Integer` because of GH-104
    val result = deserialize(numericMapJson, new TypeReference[collection.SortedMap[Integer,String]]{})
    result should equal (numericMapScala)
  }

  it should "handle key type information" in {
    val result: collection.SortedMap[UUID,Int] = deserialize("""{"e79bf81e-3902-4801-831f-d161be435787":5}""", new TypeReference[collection.SortedMap[UUID,Int]]{})
    result.keys.head shouldBe UUID.fromString("e79bf81e-3902-4801-831f-d161be435787")
  }

  it should "properly deserialize nullary values" in {
    val result = deserialize(nullValueMapJson, new TypeReference[collection.SortedMap[String, JsonNode]] {})
    result should equal (nullValueMapScala)
  }

  private val mapJson =  """{ "one": "1", "two": "2" }"""
  private val mapScala = collection.SortedMap("one"->"1","two"->"2")
  private val variantMapJson = """{ "one": "1", "two": 2 }"""
  private val variantMapScala = collection.SortedMap[String,Any]("one"->"1","two"->2)
  private val numericMapJson = """{ "1": "one", "2": "two" }"""
  private val numericMapScala = collection.SortedMap[Integer,String](Integer.valueOf(1)->"one",Integer.valueOf(2)->"two")
  private val nullValueMapJson =
    """
      |{
      | "foo": "bar",
      | "nullValue": null,
      | "intValue": 1234
      |}
    """.stripMargin
  private val nullValueMapScala = collection.SortedMap[String, JsonNode](
    "foo" -> JsonNodeFactory.instance.textNode("bar"),
    "nullValue" -> JsonNodeFactory.instance.nullNode(),
    "intValue" -> JsonNodeFactory.instance.numberNode(1234)
  )
}
