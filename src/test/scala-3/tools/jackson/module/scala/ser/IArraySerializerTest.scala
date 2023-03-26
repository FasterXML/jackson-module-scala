package com.fasterxml.jackson.module.scala.ser

import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

class IArraySerializerTest extends SerializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with IterableSerializer" should "serialize an IArray[Int]" in {
    serialize(IArray(1, 2, 3)) should be("[1,2,3]")
  }
}
