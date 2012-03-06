package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import scala.collection.{immutable, mutable}

@RunWith(classOf[JUnitRunner])
class UnsortedSetDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new UnsortedSetDeserializerModule {}

  "An ObjectMapper with the SetDeserializerModule" should "deserialize an object into a Set" in {
    val result = deserialize[Set[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a HashSet" in {
    val result = deserialize[immutable.HashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a mutable HashSet" in {
    val result = deserialize[mutable.HashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object into a LinkedHashSet" in {
    val result = deserialize[mutable.LinkedHashSet[String]](setJson)
    result should equal(setScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedSet" in {
    val result = deserialize[Set[Any]](variantSetJson)
    result should equal(variantSetScala)
  }
  
  it should "deserialize an object into a ListSet" in {
    val result = deserialize[immutable.ListSet[String]](setJson)
    result should equal (setScala)
  }

  val setJson = """[ "one", "two" ]"""
  val setScala = Set("one", "two")
  val variantSetJson = """[ "1", 2 ]"""
  val variantSetScala = Set[Any]("1", 2)
}