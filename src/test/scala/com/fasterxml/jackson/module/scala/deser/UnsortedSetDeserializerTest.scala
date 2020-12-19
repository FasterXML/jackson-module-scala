package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.collection.{immutable, mutable}

@RunWith(classOf[JUnitRunner])
class UnsortedSetDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = new UnsortedSetDeserializerModule {}

  "An ObjectMapper with the SetDeserializerModule" should "deserialize an object into a Set" in {
    val result = deserializeWithManifest[Set[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable Set" in {
    val result = deserializeWithManifest[immutable.Set[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a mutable Set" in {
    val result = deserializeWithManifest[mutable.Set[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable HashSet" in {
    val result = deserializeWithManifest[immutable.HashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a mutable HashSet" in {
    val result = deserializeWithManifest[mutable.HashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into an immutable ListSet" in {
    val result = deserializeWithManifest[immutable.ListSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a LinkedHashSet" in {
    val result = deserializeWithManifest[mutable.LinkedHashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedSet" in {
    val result = deserializeWithManifest[Set[Any]](variantSetJson)
    result should equal(variantSetScala)
  }

  it should "deserialize an object into a ListSet" in {
    val result = deserializeWithManifest[immutable.ListSet[String]](setJson)
    result should equal(setScala)
  }

  it should "keep path index if error happened" in {
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      deserializeWithManifest[immutable.ListSet[UUID]](uuidListJson)
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal(List(1))
  }

  val setJson = """[ "one", "two" ]"""
  val setScala = Set("one", "two")
  val variantSetJson = """[ "1", 2 ]"""
  val variantSetScala: Set[Any] = Set[Any]("1", 2)
}
