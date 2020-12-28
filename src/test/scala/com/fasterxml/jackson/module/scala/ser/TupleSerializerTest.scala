package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.JacksonModule

class TupleSerializerTest extends SerializerTest {
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
