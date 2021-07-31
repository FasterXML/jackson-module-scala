package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object JsonTypeSerializerTest {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "typeMarker")
  @JsonSubTypes(Array(
    new Type(value = classOf[AnnotatedFoo1], name = "foo1"),
    new Type(value = classOf[AnnotatedFoo2], name = "foo2")
  ))
  sealed trait AnnotatedFoo {
    val prop: String
  }

  case class AnnotatedFoo1(name1: String, prop: String) extends AnnotatedFoo {
    def this() = this(null, null)
  }

  case class AnnotatedFoo2(name2: String, prop: String) extends AnnotatedFoo {
    def this() = this(null, null)
  }

  case class AnnotatedCon(foo: AnnotatedFoo)

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
    val con = AnnotatedCon(AnnotatedFoo1("foo1", "foo1"))
    val mapper = newBuilder.build()
    val con1JsonStr = mapper.writeValueAsString(con)
    con1JsonStr shouldEqual """{"foo":{"typeMarker":"foo1","name1":"foo1","prop":"foo1"}}"""

    val con2 = mapper.readValue(con1JsonStr, classOf[AnnotatedCon])
    con2 shouldEqual con
  }
  it should "not duplicate fields (no annotations)" in {
    val con = Con(Foo1("foo1", "foo1"))
    val mapper = newBuilder.build()
    val con1JsonStr = mapper.writeValueAsString(con)
    con1JsonStr shouldEqual """{"foo":{"name1":"foo1","prop":"foo1"}}"""
    // we cannot deserialize this json back to Con instance as we don't have the typeMarker that we have in the previous test
  }
}
