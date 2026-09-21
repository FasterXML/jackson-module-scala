package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.scala.ser.TupleSerializerTest.OptionalTupleHolder
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JacksonModule}

@JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[TupleValueLong], name = "TupleValueLong"),
  new JsonSubTypes.Type(value = classOf[TupleValueString], name = "TupleValueString")
))
trait TupleValueBase
case class TupleValueLong(long: Long) extends TupleValueBase
case class TupleValueString(string: String) extends TupleValueBase
case class TupleContainer(tuple: (TupleValueBase,TupleValueBase))

case class OptionalTupleHolder2(tuple: (Option[Int], Option[Boolean]))

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
    result shouldEqual value
  }

  it should "deserialize using type information outside of field" in {
    val value = (TupleValueLong(1), TupleValueString("foo"))
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, new TypeReference[(TupleValueBase, TupleValueBase)]{})
    result shouldEqual value
  }

  it should "deserialize an OptionalTupleHolder" in {
    val value = OptionalTupleHolder(Some(1), Some("one"))
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, classOf[OptionalTupleHolder])
    result shouldEqual value
  }

  it should "deserialize an OptionalTupleHolder with nulls" in {
    val value = OptionalTupleHolder(None, None)
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, classOf[OptionalTupleHolder])
    result shouldEqual value
  }

  it should "deserialize an OptionalTupleHolder2 with nulls" in {
    val value = OptionalTupleHolder2(None, None)
    val json = newMapper.writeValueAsString(value)
    val result = deserialize(json, classOf[OptionalTupleHolder2])
    result shouldEqual value
  }

  it should "reject an array with more elements than the tuple has" in {
    intercept[MismatchedInputException] {
      deserialize("""[1,"a",2]""", new TypeReference[(Int, String)] {})
    }
  }

  it should "reject an array with fewer elements than the tuple has" in {
    intercept[MismatchedInputException] {
      deserialize("""[1]""", new TypeReference[(Int, String)] {})
    }
    intercept[MismatchedInputException] {
      deserialize("[]", new TypeReference[Tuple1[Int]] {})
    }
  }

  it should "reject an object where a tuple is expected" in {
    intercept[MismatchedInputException] {
      deserialize("""{"_1":1}""", new TypeReference[(Int, String)] {})
    }
  }

  it should "deserialize null to a null tuple" in {
    deserialize("null", new TypeReference[(Int, String)] {}) shouldBe null
  }

  it should "deserialize a null element" in {
    // the shape the serializer writes for (1, null)
    deserialize("""[1,null]""", new TypeReference[(Int, String)] {}) shouldEqual ((1, null))
    deserialize("""[1,null]""", new TypeReference[(Int, Option[String])] {}) shouldEqual ((1, None))
  }

  it should "apply FAIL_ON_NULL_FOR_PRIMITIVES to a null element in a primitive position" in {
    // a ClassTag keeps the Int slot primitive, where a TypeReference would erase it to Object
    val lenient = newMapper :: ClassTagExtensions
    lenient.readValue[(Int, String)]("""[null,"a"]""") shouldEqual ((0, "a"))
    val strict = newBuilder.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build() :: ClassTagExtensions
    intercept[MismatchedInputException] {
      strict.readValue[(Int, String)]("""[null,"a"]""")
    }
  }

  it should "deserialize nested tuples" in {
    val value = ((1, "z"), List(("a", Some(1)), ("b", None)))
    val json = serialize(value)
    json shouldEqual """[[1,"z"],[["a",1],["b",null]]]"""
    deserialize(json, new TypeReference[((Int, String), List[(String, Option[Int])])] {}) shouldEqual value
  }

  it should "round trip tuples of the specialized primitive kinds" in {
    deserialize(serialize((1, 2)), new TypeReference[(Int, Int)] {}) shouldEqual ((1, 2))
    deserialize(serialize((1L, 2.5)), new TypeReference[(Long, Double)] {}) shouldEqual ((1L, 2.5))
    deserialize(serialize((true, false)), new TypeReference[(Boolean, Boolean)] {}) shouldEqual ((true, false))
  }
}
