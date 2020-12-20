package com.fasterxml.jackson.module.scala.deser

import java.util.UUID
import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.collection._
import scala.compat.java8.FunctionConverters

case class JavaMapWrapper(m: java.util.HashMap[String, String])
case class MapWrapper(m: Map[String, String])

@RunWith(classOf[JUnitRunner])
class UnsortedMapDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new UnsortedMapDeserializerModule {}

  "An ObjectMapper with the UnsortedMapDeserializerModule" should "deserialize an object into a Map" in {
    val typeRef = new TypeReference[Map[String, String]] {}
    val result = deserialize(mapJson, typeRef)
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

  it should "handle conversion of null to empty collection" in {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .changeDefaultNullHandling(FunctionConverters.asJavaUnaryOperator(_ => JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)))
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
