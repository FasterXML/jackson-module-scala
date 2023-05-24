package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.scala.ser.XmlSerializationTest._
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonTest}

object XmlSerializationTest {
  case class SeqWrapper(id: String, seq: Seq[String])
  case class IteratorWrapper(id: String, iterator: Iterator[String])
}

class XmlSerializationTest extends JacksonTest {
  val module = DefaultScalaModule
  val xmlMapper = XmlMapper.builder().addModule(DefaultScalaModule).build()

  "An XmlMapper" should "serialize a Scala Iterator" in {
    val wrapper = IteratorWrapper("id1", Seq("1", "2", "3").iterator)
    val xml = xmlMapper.writeValueAsString(wrapper)
    println(xml)
  }

  it should "serialize a Scala Seq" in {
    val wrapper = SeqWrapper("id1", Seq("1", "2", "3"))
    val xml = xmlMapper.writeValueAsString(wrapper)
    println(xml)
  }
}
