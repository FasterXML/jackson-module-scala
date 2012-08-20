package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.core.`type`.TypeReference

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
@RunWith(classOf[JUnitRunner])
class TupleDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = DefaultScalaModule

  "ObjectMapper with TupleDeserialzier" should "deserialize a Tuple[Int]" in {
    val result = deserialize[Tuple1[Int]]("[1]")
    result should be (Tuple1(1))
  }

  it should "deserialize a Tuple[Double]" in {
    val result = deserialize[Tuple1[Double]]("[1.0]")
    result should be (Tuple1(1.0))
  }

  it should "deserialize a Tuple[String]" in {
    val result = deserialize[Tuple1[String]]("[\"foo\"]")
    result should be (Tuple1("foo"))
  }

  it should "deserialize a Tuple[Int,Int]" in {
    val result = deserialize[(Int, Int)]("[1,2]")
    result should be ((1,2))
  }

  it should "deserialize a Tuple[Int,Double]" in {
    val result = deserialize[(Int, Double)]("[1,2.0]")
    result should be ((1,2.0))
  }

  it should "deserialize a Tuple[Int,String]" in {
    val result = deserialize[(Int, String)]("[1,\"foo\"]")
    result should be ((1,"foo"))
  }

  it should "deserialize a Tuple3[Double,String,Int]" in {
    val result = deserialize[(Double,String,Int)]("""[3.0,"A",1]""")
    result should be ((3.0,"A",1))
  }

  it should "deserialize a list of tuples " in {
    val result = deserialize[List[(String,Double)]]("""[["foo",1.0],["bar",10.0],["baz",100.0]]""")
    result should be (List(("foo",1.0), ("bar",10.0), ("baz",100.0)))
  }

  it should "deserialize an option list of tuples " in {
    val result = deserialize[Option[List[(String,Double)]]]("""[["foo",1.0],["bar",10.0],["baz",100.0]]""")
    result should be (Some(List(("foo",1.0), ("bar",10.0), ("baz",100.0))))
  }
}