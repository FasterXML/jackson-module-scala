package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class SeqWithTupleDeserializerTest extends DeserializerTest {

  lazy val module = DefaultScalaModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into a Seq[(String, String)]" in {
    //need to deserialize with a TypeReference instead of a Class because type erasure will lose the information
    //about the reference type (the tuple)
    val result = deserialize("""[["1","2"],["3","4"]]""", new TypeReference[Seq[(String, String)]]{})
    result shouldEqual Seq("1" -> "2", "3" -> "4")
  }
}
