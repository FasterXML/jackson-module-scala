package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

class IArrayDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule
  val listJson =  "[1,2,3,4,5,6]"
  val listScala: Range.Inclusive = 1 to 6

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into an immutable Array" in {
    val result = deserialize(listJson, new TypeReference[IArray[Int]]{})
    result shouldBe an[IArray[Int]]
    result should equal (listScala)
  }

}
