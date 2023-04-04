package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.ser.TupleSerializerTest.OptionalTupleHolder
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

object TupleSerializerTest {
  case class OptionalTupleHolder(tuple: (Option[Int], Option[String]))
}

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

  it should "serialize an OptionalTupleHolder" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val result = serialize(OptionalTupleHolder(Some(1), Some("one")), mapper)
    result should be("""{"tuple":[1,"one"]}""")
  }

  it should "serialize an OptionalTupleHolder with nulls" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val result = serialize(OptionalTupleHolder(None, None), mapper)
    result should be("""{"tuple":[null,null]}""")
  }
}
