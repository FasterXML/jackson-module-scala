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

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
  @JsonSubTypes(Array(
    new Type(value = classOf[RequestSerial], name = "request serial")))
  trait RequestMessage {
    val `type`: String
    val messageReference: Int
  }

  case class RequestSerial(`type`: String = "request serial",
                           messageReference: Int) extends RequestMessage {

    require(`type` == "request serial", "Parameter 'type' is invalid.")
    require(messageReference > 0, "Empty parameter 'message_reference'.")
  }

  sealed trait Parent

  case class ChildA(dataA: String) extends Parent

  case class Wrapper(typ: String,
                     @JsonSubTypes(Array(
                       new Type(value = classOf[ChildA], name = "ChildA")
                     ))
                     @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "typ")
                     polymorphic: Parent)
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
  //https://github.com/FasterXML/jackson-module-scala/issues/207
  it should "serialize RequestSerial" in {
    val request = RequestSerial("request serial", 10)
    val mapper = newBuilder.build()
    val jsonStr = mapper.writeValueAsString(request)
    jsonStr shouldEqual """{"type":"request serial","messageReference":10}"""
    mapper.readValue(jsonStr, classOf[RequestSerial]) shouldEqual request
  }
  //https://github.com/FasterXML/jackson-module-scala/issues/200
  it should "deserialize Wrapper" in {
    val mapper = newBuilder.build()
    val json = """{"typ":"ChildA", "polymorphic" : { "dataA":"xxx"}}"""
    mapper.readValue(json, classOf[Wrapper]) shouldEqual Wrapper(typ = "ChildA", polymorphic = ChildA("xxx"))
  }
}
