package com.fasterxml.jackson.module.scala.ser

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import scala.collection._
import scala.reflect.BeanProperty
import com.fasterxml.jackson.annotation.JsonProperty
import scala.collection.immutable.ListMap

class BeanieWeenie(@BeanProperty @JsonProperty("a") var a: Int,
                   @BeanProperty @JsonProperty("b") var b: String, 
                   @BeanProperty @JsonProperty("c") var c: Boolean) {
  
}

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
  
  it should "serialize a map of beans-like objects" in {
    val result = serialize(Map("bean" -> new BeanieWeenie(1,"two",false)))
    result should (
      be ("""{"bean":{"a":1,"b":"two","c":false}}""")
    )
  }

  it should "serialize order-specified Maps in the correc order" in {
    val m = ListMap(Map((5, 1), (2, 33), (7, 22), (8, 333)).toList.sortBy(-_._2):_*)
    val result = serialize(m)
    result should be ("""{"8":333,"2":33,"7":22,"5":1}""")
  }

}