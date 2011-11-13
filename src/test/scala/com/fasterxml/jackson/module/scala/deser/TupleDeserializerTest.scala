package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.`type`.TypeReference
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class TupleDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new TupleDeserializerModule {}

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

}