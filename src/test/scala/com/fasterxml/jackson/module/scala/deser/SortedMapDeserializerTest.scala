package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

@RunWith(classOf[JUnitRunner])
class SortedMapDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new SortedMapDeserializerModule {}

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into a SortedMap" in {
    val result = deserialize[SortedMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an TreeMap" in {
    val result = deserialize[TreeMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize[SortedMap[String,Any]](variantMapJson)
    result should equal (variantMapScala)
  }

  it should "deserialize an object with numeric keys into a SortedMap" in {
    // NB: This is `java.lang.Integer` because of GH-104
    val result = deserialize[SortedMap[Integer,String]](numericMapJson)
    result should equal (numericMapScala)
  }

  it should "handle key type information" in {
    val result: SortedMap[UUID,Int] = newMapper.readValue("""{"e79bf81e-3902-4801-831f-d161be435787":5}""", new TypeReference[SortedMap[UUID,Int]]{})
    result.keys.head shouldBe UUID.fromString("e79bf81e-3902-4801-831f-d161be435787")
  }

  it should "properly deserialize nullary values" in {
    val result = deserialize[SortedMap[String, JsonNode]](nullValueMapJson)
    result should equal (nullValueMapScala)
  }

  private val mapJson =  """{ "one": "1", "two": "2" }"""
  private val mapScala = SortedMap("one"->"1","two"->"2")
  private val variantMapJson = """{ "one": "1", "two": 2 }"""
  private val variantMapScala = SortedMap[String,Any]("one"->"1","two"->2)
  private val numericMapJson = """{ "1": "one", "2": "two" }"""
  private val numericMapScala = SortedMap[Integer,String](Integer.valueOf(1)->"one",Integer.valueOf(2)->"two")
  private val nullValueMapJson =
    """
      |{
      | "foo": "bar",
      | "nullValue": null,
      | "intValue": 1234
      |}
    """.stripMargin
  private val nullValueMapScala = SortedMap[String, JsonNode](
    "foo" -> JsonNodeFactory.instance.textNode("bar"),
    "nullValue" -> JsonNodeFactory.instance.nullNode(),
    "intValue" -> JsonNodeFactory.instance.numberNode(1234)
  )
}
