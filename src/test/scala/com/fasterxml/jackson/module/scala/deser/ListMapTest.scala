package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{DeserializationFeature, SerializationFeature}
import com.fasterxml.jackson.module.scala.{BaseSpec, DefaultScalaModule}
import org.scalatest.prop.TableDrivenPropertyChecks

import java.io.StringWriter
import scala.collection.immutable.{ListMap, Queue, TreeMap}

// taken from https://github.com/dejanlokar1/serialization_problem/blob/master/src/test/scala/SerializationTest.scala
// test for https://github.com/FasterXML/jackson-databind/issues/2422

class ListMapTest extends BaseSpec with TableDrivenPropertyChecks {
  private val mapper = {
    val builder = JsonMapper.builder().addModule(new DefaultScalaModule)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.INDENT_OUTPUT)
    builder.build()
  }

  case class SampleCaseClass(map: Map[String, String] = Map(), seq: Seq[String] = List())

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

  private def serialize(value: Any): String = {
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }
}
