package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty, JsonSubTypes, JsonTypeInfo}
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{SerializerProvider, ValueSerializer}
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.annotation.meta.getter
import scala.beans.BeanProperty
import scala.collection._
import scala.collection.immutable.ListMap

class BeanieWeenie(@BeanProperty @JsonProperty("a") var a: Int,
                   @BeanProperty @JsonProperty("b") var b: String,
                   @BeanProperty @JsonProperty("c") var c: Boolean)

class NonEmptyMaps {
  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def emptyMap = Map.empty[String,Int]

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def nonEmptyMap = Map("x"->1)
}

class TupleKeySerializer extends ValueSerializer[Product] {
  override def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    val objectMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    jgen.writeName(objectMapper.writeValueAsString(value))
  }
}

case class KeySerializerMap(
  @(JsonSerialize @getter)(keyUsing = classOf[TupleKeySerializer])
  keySerializerMap: Map[(String,String),Int])

@JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[MapValueDouble], name = "MapValueDouble"),
  new JsonSubTypes.Type(value = classOf[MapValueString], name = "MapValueString")
))
abstract class MapValueBase {}
case class MapValueDouble(value: Double) extends MapValueBase
case class MapValueString(value: String) extends MapValueBase

case class MapValueBaseWrapper(map: Map[String, MapValueBase])

//see also MapScala2SerializerTest for tests that only pass with Scala2
class MapSerializerTest extends SerializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

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
    serialize(KeySerializerMap(Map(("a","b")->1))) should be ("""{"keySerializerMap":{"[\"a\",\"b\"]":1}}""")
  }

  it should "correctly serialize type information" in {
    val wrapper = MapValueBaseWrapper(Map("Double" -> MapValueDouble(1.0), "String" -> MapValueString("word")))
    serialize(wrapper) should be ("""{"map":{"Double":{"type":"MapValueDouble","value":1.0},"String":{"type":"MapValueString","value":"word"}}}""")
  }
}
