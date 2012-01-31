package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class SortedMapDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new SortedMapDeserializerModule {}

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into a SortedMap" in {
    val result = deserialize[SortedMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an TreeMap" in {
    val result = deserialize[TreeMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize[SortedMap[String,Any]](variantMapJson)
    result should equal (variantMapScala)
  }

  ignore should "deserialize an object with numeric keys into a SortedMap" in {
    val result = deserialize[SortedMap[Int,String]](numericMapJson)
    result should equal (numericMapScala)
  }

  val mapJson =  """{ "one": "1", "two": "2" }"""
  val mapScala = SortedMap("one"->"1","two"->"2")
  val variantMapJson = """{ "one": "1", "two": 2 }"""
  val variantMapScala = SortedMap[String,Any]("one"->"1","two"->2)
  val numericMapJson = """{ "1": "one", "2": "two" }"""
  val numericMapScala = SortedMap(1->"one",2->"two")
}