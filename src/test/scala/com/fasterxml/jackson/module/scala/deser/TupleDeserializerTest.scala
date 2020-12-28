package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

@JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[TupleValueLong], name = "TupleValueLong"),
  new JsonSubTypes.Type(value = classOf[TupleValueString], name = "TupleValueString")
))
trait TupleValueBase
case class TupleValueLong(long: Long) extends TupleValueBase
case class TupleValueString(string: String) extends TupleValueBase
case class TupleContainer(tuple: (TupleValueBase,TupleValueBase))

class TupleDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "ObjectMapper with TupleDeserialzier" should "deserialize a Tuple[Int]" in {
    val result = deserialize("[1]", new TypeReference[Tuple1[Int]]{})
    result should be (Tuple1(1))
  }

  it should "deserialize a Tuple[Double]" in {
    val result = deserialize("[1.0]", new TypeReference[Tuple1[Double]]{})
    result should be (Tuple1(1.0))
  }

  it should "deserialize a Tuple[String]" in {
    val result = deserialize("[\"foo\"]", new TypeReference[Tuple1[String]]{})
    result should be (Tuple1("foo"))
  }

  it should "deserialize a Tuple[Int,Int]" in {
    val result = deserialize("[1,2]", new TypeReference[(Int, Int)]{})
    result should be ((1,2))
  }

  it should "deserialize a Tuple[Int,Double]" in {
    val result = deserialize("[1,2.0]", new TypeReference[(Int, Double)]{})
    result should be ((1,2.0))
  }

  it should "deserialize a Tuple[Int,String]" in {
    val result = deserialize("[1,\"foo\"]", new TypeReference[(Int, String)]{})
    result should be ((1,"foo"))
  }

  it should "deserialize a Tuple3[Double,String,Int]" in {
    val result = deserialize("""[3.0,"A",1]""", new TypeReference[(Double,String,Int)]{})
    result should be ((3.0,"A",1))
  }

  it should "deserialize a list of tuples " in {
    val result = deserialize("""[["foo",1.0],["bar",10.0],["baz",100.0]]""", new TypeReference[List[(String,Double)]]{})
    result should be (List(("foo",1.0), ("bar",10.0), ("baz",100.0)))
  }

  it should "deserialize an option list of tuples " in {
    val result = deserialize("""[["foo",1.0],["bar",10.0],["baz",100.0]]""", new TypeReference[Option[List[(String,Double)]]]{})
    result should be (Some(List(("foo",1.0), ("bar",10.0), ("baz",100.0))))
  }

  it should "deserialize using type information" in {
    val value = TupleContainer(TupleValueLong(1), TupleValueString("foo"))
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, new TypeReference[TupleContainer]{})
    result should be (value)
  }

  it should "deserialize using type information outside of field" in {
    val value = (TupleValueLong(1), TupleValueString("foo"))
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, new TypeReference[(TupleValueBase, TupleValueBase)]{})
    result should be (value)
  }
}
