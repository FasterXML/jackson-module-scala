package com.fasterxml.jackson.module.scala.ser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.JacksonModule

@RunWith(classOf[JUnitRunner])
class TupleSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {
  lazy val module = new JacksonModule with TupleSerializerModule

  "An ObjectMapper" should "serialize a Tuple2" in {
    val result = serialize("A" -> 1)
    result should be ("""["A",1]""")
  }

  it should "serialize a Tuple3" in {
    val result = serialize((3.0, "A", 1))
    result should be ("""[3.0,"A",1]""")
  }

}