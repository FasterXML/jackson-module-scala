package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.DefaultScalaModule

class RecordTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "not affect Java Record deserialization" in {
    deserialize("1", classOf[Option[Int]]) should be(Some(1))
    deserialize("1", classOf[Option[Int]]) should be(Option(1))
    deserialize("null", classOf[Option[Int]]) should be(None)
  }
}
