package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import collection.LinearSeq
import collection.mutable
import collection.immutable.Queue
import com.fasterxml.jackson.module.scala.JacksonModule

@RunWith(classOf[JUnitRunner])
class SeqDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new JacksonModule with SeqDeserializerModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into a Seq" in {
    // The temporary object is necessary to force the cast back to the concrete type.
    // Otherwise the value is elided directly to ShouldMatcher, which will take any Seq
    // TODO: Would the JVM ever elide this anyway?
    val result = deserialize[collection.Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an IndexedSeq" in {
    val result = deserialize[IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable IndexedSeq" in {
    val result = deserialize[mutable.IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Vector" in {
    val result = deserialize[Vector[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a ResizableArray" in {
    val result = deserialize[mutable.ResizableArray[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserialize[mutable.ArraySeq[Int]](listJson)
    //result should equal (listScala) TODO: fails to compile in scala 2.10.0-M7 -- reanble for final release
  }

  it should "deserialize a list into a LinearSeq" in {
    val result = deserialize[LinearSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable LinearSeq" in {
    val result = deserialize[mutable.LinearSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a List" in {
    val result = deserialize[List[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Stream" in {
    val result = deserialize[Stream[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a MutableList" in {
    val result = deserialize[mutable.MutableList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Queue" in {
    val result = deserialize[Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Queue" in {
    val result = deserialize[mutable.Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Buffer" in {
    val result = deserialize[mutable.Buffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayBuffer" in {
    val result = deserialize[mutable.ArrayBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ListBuffer" in {
    val result = deserialize[mutable.ListBuffer[Int]](listJson)
    result should equal (listScala)
  }

  val listJson =  "[1,2,3,4,5,6]"
  val listScala = (1 to 6)
}