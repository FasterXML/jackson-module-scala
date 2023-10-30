package com.fasterxml.jackson.module.scala.yaml

import com.fasterxml.jackson.databind.{Module, ObjectMapper}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.yaml.YamlSpec.Person
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonTest}

object YamlSpec {
  case class Person(name: String, id: Int, titles: List[String], permanent: Boolean)
}

class YamlSpec extends JacksonTest {
  override def module: Module = DefaultScalaModule

  behavior of "ObjectMapper with YAML and DefaultScalaModule"

  it should "serialize/deserialize Person" in {
    val p = Person("cb", 12, List("h1", "h2"), true)
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)

    val str = mapper.writeValueAsString(p)
    val p1 = mapper.readValue(str, classOf[Person])
    p1 shouldEqual p
  }
}
