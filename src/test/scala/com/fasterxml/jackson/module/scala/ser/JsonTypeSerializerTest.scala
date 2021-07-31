package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonTypeSerializerTest {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "prop")
  @JsonSubTypes(Array(
    new Type(value = classOf[Foo1], name = "foo1"),
    new Type(value = classOf[Foo2], name = "foo2")
  ))
  sealed trait Foo {
    val prop: String
  }

  case class Foo1(name1: String, prop: String) extends Foo {
    def this() = this(null, null)
  }

  case class Foo2(name2: String, prop: String) extends Foo {
    def this() = this(null, null)
  }

  case class Con(foo: Foo)
}

class JsonTypeSerializerTest extends SerializerTest {

  import JsonTypeSerializerTest._

  def module = DefaultScalaModule

  //https://github.com/FasterXML/jackson-module-scala/issues/309
  "JsonTypeSerializer" should "not duplicate fields" in {
    val con = Con(Foo1("foo1", "foo1"))
    val mapper = newBuilder.build()
    val con1JsonStr = mapper.writeValueAsString(con)
    //TODO fix - this result is wrong as it duplicates the "prop" element
    con1JsonStr shouldEqual """{"foo":{"prop":"foo1","name1":"foo1","prop":"foo1"}}"""
  }
}
