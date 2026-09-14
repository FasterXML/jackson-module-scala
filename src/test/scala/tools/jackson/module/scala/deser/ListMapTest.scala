package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{DeserializationFeature, SerializationFeature}
import tools.jackson.module.scala.{DefaultScalaModule, MapModule, ScalaModule}
import org.scalatest.prop.TableDrivenPropertyChecks
import tools.jackson.module.scala.BaseSpec

import java.io.StringWriter
import scala.collection.immutable.{ListMap, Queue, TreeMap}
import scala.collection.mutable

// taken from https://github.com/dejanlokar1/serialization_problem/blob/master/src/test/scala/SerializationTest.scala
// test for https://github.com/FasterXML/jackson-databind/issues/2422

// deserialized, so declared where Jackson can construct them - an inner case class cannot be
case class ListMapHolder(map: ListMap[String, Int])
case class MutableListMapHolder(map: mutable.ListMap[String, Int])

class ListMapTest extends BaseSpec with TableDrivenPropertyChecks {
  private val mapper = {
    val moduleBuilder = ScalaModule.builder().addAllBuiltinModules()
    val builder = JsonMapper.builder().addModule(moduleBuilder.build())
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.INDENT_OUTPUT)
    builder.build()
  }

  case class SampleCaseClass(map: Map[String, String] = Map(), seq: Seq[String] = List())

  // enough keys that a hash-based map would not happen to keep them in order
  private val orderedKeys = ('a' to 'p').map(_.toString)
  private val orderedJson = jsonOf(orderedKeys.zipWithIndex)

  private def jsonOf(pairs: Seq[(String, Int)]): String =
    pairs.map { case (k, v) => s""""$k":$v""" }.mkString("{", ",", "}")

  "Map Serialization" should "handle problematic list map" in {
    val sampleListMap = ListMap("foo" -> "bar")
    val sampleCaseClass = SampleCaseClass(map = sampleListMap)

    // list map can be serialized
    serialize(sampleListMap) shouldBe """{"foo":"bar"}"""
    // list map in a case class can not be serialized
    serialize(sampleCaseClass) shouldBe """{"map":{"foo":"bar"},"seq":[]}"""
  }

  "Map Serialization" should "handle working maps" in {
    val maps = Table(
      "Map implementations",
      Map("foo" -> "bar"),
      TreeMap("foo" -> "bar")
    )

    forAll(maps) { map =>
      val sampleCaseClass = SampleCaseClass(map = map)
      serialize(sampleCaseClass) shouldBe """{"map":{"foo":"bar"},"seq":[]}"""
    }
  }

  "Map Serialization" should "handle working sequences" in {
    val sequences = Table(
      "Sequence implementations",
      List("foo"),
      Stream("foo"),
      Queue("foo"),
      Vector("foo")
    )

    forAll(sequences) { seq =>
      val sampleCaseClass = SampleCaseClass(seq = seq)
      serialize(sampleCaseClass) shouldBe """{"map":{},"seq":["foo"]}"""
    }
  }

  "Map Deserialization" should "keep the JSON order of the keys in an immutable ListMap" in {
    val typeRef = new TypeReference[ListMap[String, Int]] {}
    val result = mapper.readValue(orderedJson, typeRef)
    result shouldBe a[ListMap[_, _]]
    result.keys.toSeq shouldBe orderedKeys
    result.values.toSeq shouldBe orderedKeys.indices
  }

  it should "keep the JSON order of the keys in a case class field typed as an immutable ListMap" in {
    val result = mapper.readValue(s"""{"map":$orderedJson}""", classOf[ListMapHolder])
    result.map shouldBe a[ListMap[_, _]]
    result.map.keys.toSeq shouldBe orderedKeys
  }

  it should "deserialize an object into a mutable ListMap" in {
    val typeRef = new TypeReference[mutable.ListMap[String, Int]] {}
    val result = mapper.readValue(orderedJson, typeRef)
    result shouldBe a[mutable.ListMap[_, _]]
    // a mutable ListMap makes no promise about its iteration order, so only the contents are checked
    result shouldBe orderedKeys.zipWithIndex.toMap
  }

  it should "deserialize an object into a case class field typed as a mutable ListMap" in {
    val result = mapper.readValue(s"""{"map":$orderedJson}""", classOf[MutableListMapHolder])
    result.map shouldBe a[mutable.ListMap[_, _]]
    result.map shouldBe orderedKeys.zipWithIndex.toMap
  }

  "Map round trip" should "keep the order of an immutable ListMap through JSON and back" in {
    val original = ListMap(orderedKeys.zipWithIndex: _*)
    val json = serialize(original)
    json shouldBe orderedJson
    val result = mapper.readValue(json, new TypeReference[ListMap[String, Int]] {})
    result.toSeq shouldBe original.toSeq
  }

  it should "keep the order of an immutable ListMap in a case class through JSON and back" in {
    val original = ListMapHolder(ListMap(orderedKeys.reverse.zipWithIndex: _*))
    val json = serialize(original)
    json shouldBe s"""{"map":${jsonOf(original.map.toSeq)}}"""
    val result = mapper.readValue(json, classOf[ListMapHolder])
    result.map.toSeq shouldBe original.map.toSeq
  }

  private def serialize(value: Any): String = {
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }
}
