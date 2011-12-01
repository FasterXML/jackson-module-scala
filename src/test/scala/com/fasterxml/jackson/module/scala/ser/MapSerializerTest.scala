package com.fasterxml.jackson.module.scala.ser

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import scala.collection._

@RunWith(classOf[JUnitRunner])
class MapSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new MapSerializerModule {}

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

}