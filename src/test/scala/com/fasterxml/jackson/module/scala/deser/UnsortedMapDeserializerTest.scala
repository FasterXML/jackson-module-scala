package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.codehaus.jackson.`type`.TypeReference
import scala.collection.immutable.HashMap
import scala.collection.mutable

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class UnsortedMapDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new UnsortedMapDeserializerModule {}

  "An ObjectMapper with the UnsortedMapDeserializerModule" should "deserialize an object into an Map" in {
    val result = deserialize(mapJson, new TypeReference[Map[String,String]] {})
    result should equal (mapScala)
  }

  it should "deserialize an object into an HashMap" in {
    val result = deserialize(mapJson, new TypeReference[HashMap[String,String]] {})
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable HashMap" in {
    val result = deserialize(mapJson, new TypeReference[mutable.HashMap[String,String]] {})
    result should equal (mapScala)
  }

  it should "deserialize an object into a LinkedHashMap" in {
    val result = deserialize(mapJson, new TypeReference[mutable.LinkedHashMap[String,String]] {})
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize(variantMapJson, new TypeReference[Map[String,Any]]{})
    result should equal (variantMapScala)
  }

  val mapJson =  """{ "one": "1", "two": "2" }"""
  val mapScala = Map("one"->"1","two"->"2")
  val variantMapJson = """{ "one": "1", "two": 2 }"""
  val variantMapScala = Map[String,Any]("one"->"1","two"->2)
}