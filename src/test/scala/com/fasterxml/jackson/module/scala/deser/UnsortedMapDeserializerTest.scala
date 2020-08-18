package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.collection._

case class JavaMapWrapper(m: java.util.HashMap[String, String])
case class MapWrapper(m: Map[String, String])

@RunWith(classOf[JUnitRunner])
class UnsortedMapDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new UnsortedMapDeserializerModule {}

  "An ObjectMapper with the UnsortedMapDeserializerModule" should "deserialize an object into a Map" in {
    val result = deserialize[Map[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable Map" in {
    val result = deserialize[immutable.Map[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable Map" in {
    val result = deserialize[mutable.Map[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a GenMap" in {
    val result = deserialize[GenMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable HashMap" in {
    val result = deserialize[immutable.HashMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable HashMap" in {
    val result = deserialize[mutable.HashMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable ListMap" in {
    val result = deserialize[immutable.ListMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable ListMap" in {
    val result = deserialize[mutable.ListMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable LinkedHashMap" in {
    val result = deserialize[mutable.LinkedHashMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a concurrent TrieMap" in {
    import overrides._
    val result = deserialize[TrieMap[String, String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize[Map[String, Any]](variantMapJson)
    result should equal (variantMapScala)
  }

  it should "handle key type information" in {
    val result: Map[UUID,Int] = newMapper.readValue("""{"e79bf81e-3902-4801-831f-d161be435787":5}""", new TypeReference[Map[UUID,Int]]{})
    result.keys.head shouldBe (UUID.fromString("e79bf81e-3902-4801-831f-d161be435787"))
  }

  it should "properly deserialize nullary values" in {
    val result = deserialize[Map[String, JsonNode]](nullValueMapJson)
    result should equal (nullValueMapScala)
  }

  it should "handle conversion of null to empty collection" in {
    val mapper = newBuilder.changeDefaultNullHandling(_ => JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
      .build()
    val json = """{"m": null}"""
    val result1 = mapper.readValue(json, classOf[JavaMapWrapper])
    result1 shouldEqual JavaMapWrapper(new java.util.HashMap[String, String]())
    val result2 = mapper.readValue(json, classOf[MapWrapper])
    result2 shouldEqual MapWrapper(Map.empty[String, String])
  }

  private val mapJson =  """{ "one": "1", "two": "2" }"""
  private val mapScala = Map("one"->"1","two"->"2")
  private val variantMapJson = """{ "one": "1", "two": 2 }"""
  private val variantMapScala = Map[String, Any]("one"->"1","two"->2)
  private val nullValueMapJson =
    """
      |{
      | "foo": "bar",
      | "nullValue": null,
      | "intValue": 1234
      |}
    """.stripMargin
  private val nullValueMapScala = Map[String, JsonNode](
    "foo" -> JsonNodeFactory.instance.textNode("bar"),
    "nullValue" -> JsonNodeFactory.instance.nullNode(),
    "intValue" -> JsonNodeFactory.instance.numberNode(1234)
  )
}
