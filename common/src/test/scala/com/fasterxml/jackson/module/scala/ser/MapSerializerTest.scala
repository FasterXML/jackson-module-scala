package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonInclude.Include
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection._
import JavaConverters._
import scala.beans.BeanProperty
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo, JsonInclude, JsonProperty}
import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import scala.collection.immutable.ListMap
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import annotation.meta.getter
import com.fasterxml.jackson.databind.{SerializationFeature, SerializerProvider, JsonSerializer}
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class BeanieWeenie(@BeanProperty @JsonProperty("a") var a: Int,
                   @BeanProperty @JsonProperty("b") var b: String, 
                   @BeanProperty @JsonProperty("c") var c: Boolean) {
  
}

class NonEmptyMaps {

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def emptyMap = Map.empty[String,Int]

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def nonEmptyMap = Map("x"->1)

}

class TupleKeySerializer extends JsonSerializer[Product] {
  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider) {
    val stringWriter = new java.io.StringWriter()
    val valueJgen = jgen.getCodec.getFactory.createJsonGenerator(stringWriter)

    valueJgen.writeObject(value)
    jgen.writeFieldName(stringWriter.toString)
  }
}

case class KeySerializerMap(
  @(JsonSerialize @getter)(keyUsing = classOf[TupleKeySerializer])
  keySerializerMap: Map[(String,String),Int] )

@JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[MapValueDouble], name = "MapValueDouble"),
  new JsonSubTypes.Type(value = classOf[MapValueString], name = "MapValueString")
))
abstract class MapValueBase {}
case class MapValueDouble(value: Double) extends MapValueBase
case class MapValueString(value: String) extends MapValueBase


@RunWith(classOf[JUnitRunner])
class MapSerializerTest extends SerializerTest {

  lazy val module = DefaultScalaModule

  "MapSerializerModule" should "serialize a map" in {
    val result = serialize(Map("a" -> 1, "b" -> "two", "c" -> false))
    result should (
      be ("""{"a":1,"b":"two","c":false}""") or
      be ("""{"a":1,"c":false,"b":"two"}""") or
      be ("""{"b":"two","a":1,"c":false}""") or
      be ("""{"b":"two","c":false,"a":1}""") or
      be ("""{"c":false,"a":1,"b":"two"}""") or
      be ("""{"c":false,"b":"two","a":1}""")
    )
  }

  it should "serialize a mutable map" in {
    val result = serialize(mutable.Map("a" -> 1, "b" -> "two", "c" -> false))
    result should (
      be ("""{"a":1,"b":"two","c":false}""") or
      be ("""{"a":1,"c":false,"b":"two"}""") or
      be ("""{"b":"two","a":1,"c":false}""") or
      be ("""{"b":"two","c":false,"a":1}""") or
      be ("""{"c":false,"a":1,"b":"two"}""") or
      be ("""{"c":false,"b":"two","a":1}""")
    )
  }
  
  it should "serialize a map of beans-like objects" in {
    val result = serialize(Map("bean" -> new BeanieWeenie(1,"two",false)))
    result should be ("""{"bean":{"a":1,"b":"two","c":false}}""")
  }

  it should "serialize order-specified Maps in the correct order" in {
    val m = ListMap(Map((5, 1), (2, 33), (7, 22), (8, 333)).toList.sortBy(-_._2):_*)
    val result = serialize(m)
    result should be ("""{"8":333,"2":33,"7":22,"5":1}""")
  }

  it should "honor JsonInclude(NON_EMPTY)" in {
    serialize(new NonEmptyMaps) should be ("""{"nonEmptyMap":{"x":1}}""")
  }

  it should "honor KeySerializer annotations" in {
    serialize(new KeySerializerMap(Map(("a","b")->1))) should be ("""{"keySerializerMap":{"[\"a\",\"b\"]":1}}""")
  }

  it should "correctly serialize type information" in {
    val wrapper = new {
      val map = Map.apply[String, MapValueBase]("Double" -> MapValueDouble(1.0), "String" -> MapValueString("word"))
      //val map = ImmutableMap.of[String, MapValueBase]("Double", MapValueDouble(1.0), "String", MapValueString("word"))
    }
    serialize(wrapper) should be ("""{"map":{"Double":{"type":"MapValueDouble","value":1.0},"String":{"type":"MapValueString","value":"word"}}}""")
  }

  it should "suppress None when WRITE_NULL_MAP_VALUES is active" in {
    val wrapper = new {
      val map = Map("key" -> None)
    }
    val m = mapper.copy()
    m.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    val v = m.writeValueAsString(wrapper)
    v shouldBe """{"map":{}}"""
  }
}