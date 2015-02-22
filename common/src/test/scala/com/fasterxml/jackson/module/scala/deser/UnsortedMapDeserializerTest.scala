package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import scala.collection.immutable.HashMap
import scala.collection.mutable
import com.fasterxml.jackson.core.`type`.TypeReference
import java.util.UUID

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class UnsortedMapDeserializerTest extends DeserializerTest {

  lazy val module = new UnsortedMapDeserializerModule {}

  "An ObjectMapper with the UnsortedMapDeserializerModule" should "deserialize an object into an Map" in {
    val result = deserialize[Map[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into an HashMap" in {
    val result = deserialize[HashMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable Map" in {
    val result = deserialize[mutable.Map[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a mutable HashMap" in {
    val result = deserialize[mutable.HashMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object into a LinkedHashMap" in {
    val result = deserialize[mutable.LinkedHashMap[String,String]](mapJson)
    result should equal (mapScala)
  }

  it should "deserialize an object with variable value types into a variable UnsortedMap" in {
    val result = deserialize[Map[String,Any]](variantMapJson)
    result should equal (variantMapScala)
  }

  it should "handle key type information" in {
    val result: Map[UUID,Int] = mapper.readValue("""{"e79bf81e-3902-4801-831f-d161be435787":5}""", new TypeReference[Map[UUID,Int]]{})
    result.keys.head shouldBe (UUID.fromString("e79bf81e-3902-4801-831f-d161be435787"))
  }

  val mapJson =  """{ "one": "1", "two": "2" }"""
  val mapScala = Map("one"->"1","two"->"2")
  val variantMapJson = """{ "one": "1", "two": 2 }"""
  val variantMapScala = Map[String,Any]("one"->"1","two"->2)
}