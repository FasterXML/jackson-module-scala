package com.fasterxml.jackson.module.scala.ser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MapSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new MapSerializerModule {}

  "MapSerializerModule" should "serialize a map" in {
    val result = serialize(Map("a" -> 1, "b" -> "two", "c" -> false))
    result should be ("""{"a":1,"b":"two","c":false}""")
  }

}