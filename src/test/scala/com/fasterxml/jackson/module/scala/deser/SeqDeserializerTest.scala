package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.codehaus.jackson.`type`.TypeReference
import collection.LinearSeq
import collection.mutable
import collection.immutable.Queue
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.JacksonModule

@RunWith(classOf[JUnitRunner])
class SeqDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  def module = new JacksonModule with SeqDeserializerModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into an IndexedSeq" in {
    // The temporary object is necessary to force the cast back to the concrete type.
    // Otherwise the value is elided directly to ShouldMatcher, which will take any Seq
    // TODO: Would the JVM ever elide this anyway?
    val result = deserialize(listJson, new TypeReference[IndexedSeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable IndexedSeq" in {
    val result = deserialize(listJson, new TypeReference[mutable.IndexedSeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a Vector" in {
    val result = deserialize(listJson, new TypeReference[Vector[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a ResizableArray" in {
    val result = deserialize(listJson, new TypeReference[mutable.ResizableArray[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserialize(listJson, new TypeReference[mutable.ArraySeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a LinearSeq" in {
    val result = deserialize(listJson, new TypeReference[LinearSeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable LinearSeq" in {
    val result = deserialize(listJson, new TypeReference[mutable.LinearSeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a List" in {
    val result = deserialize(listJson, new TypeReference[List[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a Stream" in {
    val result = deserialize(listJson, new TypeReference[Stream[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a MutableList" in {
    val result = deserialize(listJson, new TypeReference[mutable.MutableList[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Queue" in {
    val result = deserialize(listJson, new TypeReference[Queue[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Queue" in {
    val result = deserialize(listJson, new TypeReference[mutable.Queue[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Buffer" in {
    val result = deserialize(listJson, new TypeReference[mutable.Buffer[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayBuffer" in {
    val result = deserialize(listJson, new TypeReference[mutable.ArrayBuffer[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ListBuffer" in {
    val result = deserialize(listJson, new TypeReference[mutable.ListBuffer[Int]] {})
    result should equal (listScala)
  }

  val listJson =  "[1,2,3,4,5,6]"
  val listScala = (1 to 6)
}