package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.collection.immutable.TreeSeqMap
import scala.collection.mutable

class Map2SerializerTest extends SerializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "MapSerializerModule" should "serialize a TreeSeqMap" in {
    val result = serialize(TreeSeqMap("a" -> 1, "b" -> "two", "c" -> false))
    result shouldEqual """{"a":1,"b":"two","c":false}"""
  }

  it should "serialize a CollisionProofHashMap" in {
    val result = serialize(mutable.CollisionProofHashMap("a" -> 1, "b" -> "two", "c" -> false))
    result shouldEqual """{"a":1,"b":"two","c":false}"""
  }
}
