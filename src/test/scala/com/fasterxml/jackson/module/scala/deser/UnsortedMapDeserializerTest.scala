package com.fasterxml.jackson.module.scala.deser

import java.util.UUID
import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.scalatest.OptionValues

import scala.collection._

case class JavaMapWrapper(m: java.util.HashMap[String, String])
case class MapWrapper(m: Map[String, String])
object StringMapTypeReference extends TypeReference[Map[String, String]]

class UnsortedMapDeserializerTest extends DeserializerTest with OptionValues {

  lazy val module: JacksonModule = new UnsortedMapDeserializerModule {}

  "An ObjectMapper with the UnsortedMapDeserializerModule" should "deserialize an object into a Map" in {
    val typeRef = new TypeReference[Map[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a Map (TypeReference not declared as anonymous class)" in {
    val result = deserialize(mapJson, StringMapTypeReference)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable Map" in {
    val typeRef = new TypeReference[immutable.Map[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable Map" in {
    val typeRef = new TypeReference[mutable.Map[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a GenMap" in {
    val typeRef = new TypeReference[GenMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable HashMap" in {
    val typeRef = new TypeReference[immutable.HashMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable HashMap" in {
    val typeRef = new TypeReference[mutable.HashMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into an immutable ListMap" in {
    val typeRef = new TypeReference[immutable.ListMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable ListMap" in {
    val typeRef = new TypeReference[mutable.ListMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable LinkedHashMap" in {
    val typeRef = new TypeReference[mutable.LinkedHashMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object into a concurrent TrieMap" in {
    import overrides._
    val typeRef = new TypeReference[TrieMap[String, String]] {}
    val result = deserialize(mapJson, typeRef)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val typeRef = new TypeReference[Map[String, Any]] {}
    val result = deserialize(variantMapJson, typeRef)
    result should equal (variantMapScala)
  }

  it should "handle key type information" in {
    val result: Map[UUID,Int] = newMapper.readValue("""{"e79bf81e-3902-4801-831f-d161be435787":5}""", new TypeReference[Map[UUID,Int]]{})
    result.keys.head shouldBe (UUID.fromString("e79bf81e-3902-4801-831f-d161be435787"))
  }

  it should "properly deserialize nullary values" in {
    val typeRef = new TypeReference[Map[String, JsonNode]] {}
    val result = deserialize(nullValueMapJson, typeRef)
    result should equal (nullValueMapScala)
  }

  it should "handle AS_NULL" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
    val json = """{"m": null}"""
    val result1 = mapper.readValue(json, classOf[JavaMapWrapper])
    result1 shouldEqual JavaMapWrapper(new java.util.HashMap[String, String]())
    val result2 = mapper.readValue(json, classOf[MapWrapper])
    result2 shouldEqual MapWrapper(Map.empty)
  }

  it should "deserialize Map (Object values, duplicate keys - optional mode)" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, classOf[Map[String, Any]])
    } finally {
      parser.close()
    }
    read("1") shouldEqual 123
    val expected = new java.util.ArrayList[Object]()
    expected.add(java.lang.Integer.valueOf(123))
    expected.add(java.lang.Double.valueOf(123.456))
    read("2") shouldEqual expected
  }

  it should "deserialize mutable Map (Object values, duplicate keys - optional mode)" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    val json = """{"1": 123, "2": 123, "2": 123.456}"""
    val parser = new WithDupsParser(mapper.createParser(json))
    val read = try {
      mapper.readValue(parser, classOf[mutable.Map[String, Any]])
    } finally {
      parser.close()
    }
    read("1") shouldEqual 123
    val expected = new java.util.ArrayList[Object]()
    expected.add(java.lang.Integer.valueOf(123))
    expected.add(java.lang.Double.valueOf(123.456))
    read.remove("2").value shouldEqual expected
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
