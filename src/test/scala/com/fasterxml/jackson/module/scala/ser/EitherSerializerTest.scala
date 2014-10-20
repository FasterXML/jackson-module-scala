package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.EitherJsonTestSupport
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EitherSerializerTest extends SerializerTest with EitherJsonTestSupport {

  val module = DefaultScalaModule


  "EitherSerializer" should "be able to serialize right with string" in {
    serialize(Right(str)) should be (s"""{"r":"$str"}""")
  }

  it should "be able to serialize left with string" in {
    serialize(Left(str)) should be (s"""{"l":"$str"}""")
  }

  it should "be able to serialize right with null value" in {
    serialize(Right(null)) should be (s"""{"r":null}""")
  }

  it should "be able to serialize left with null value" in {
    serialize(Left(null)) should be (s"""{"l":null}""")
  }

  it should "be able to serialize Right with complex objects" in {
    serialize(Right(obj)) should be (s"""{"r":${serialize(obj)}}""")
  }

  it should "be able to serialize Left with complex objects" in {
    serialize(Left(obj)) should be (s"""{"l":${serialize(obj)}}""")
  }
}
