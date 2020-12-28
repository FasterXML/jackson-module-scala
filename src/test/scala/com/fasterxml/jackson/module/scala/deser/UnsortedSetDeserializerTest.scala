package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference

import java.util.UUID
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.collection.{immutable, mutable}

class UnsortedSetDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new UnsortedSetDeserializerModule {}

  "An ObjectMapper with the SetDeserializerModule" should "deserialize an object into a Set" in {
    val result = deserialize(setJson, classOf[Set[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable Set" in {
    val result = deserialize(setJson, classOf[immutable.Set[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into a mutable Set" in {
    val result = deserialize(setJson, classOf[mutable.Set[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable HashSet" in {
    val result = deserialize(setJson, classOf[immutable.HashSet[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into a mutable HashSet" in {
    val result = deserialize(setJson, classOf[mutable.HashSet[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable ListSet" in {
    val result = deserialize(setJson, classOf[immutable.ListSet[String]])
    result should equal(setScala)
  }

  it should "deserialize an object into a LinkedHashSet" in {
    val result = deserialize(setJson, classOf[mutable.LinkedHashSet[String]])
    result should equal(setScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedSet" in {
    val result = deserialize(variantSetJson, classOf[Set[Any]])
    result should equal(variantSetScala)
  }

  it should "deserialize an object into a ListSet" in {
    val result = deserialize(setJson, classOf[immutable.ListSet[String]])
    result should equal(setScala)
  }

  it should "keep path index if error happened" in {
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      newMapper.readValue(uuidListJson, new TypeReference[immutable.ListSet[UUID]] {})
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal(List(1))
  }

  val setJson = """[ "one", "two" ]"""
  val setScala = Set("one", "two")
  val variantSetJson = """[ "1", 2 ]"""
  val variantSetScala: Set[Any] = Set[Any]("1", 2)
}
