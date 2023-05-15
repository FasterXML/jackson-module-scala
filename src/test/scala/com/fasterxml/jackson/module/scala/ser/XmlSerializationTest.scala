package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.scala.ser.XmlSerializationTest.IteratorWrapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonTest}

object XmlSerializationTest {
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
}
