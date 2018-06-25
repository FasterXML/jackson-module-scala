package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.immutable.Queue
import scala.collection.{LinearSeq, mutable}

@RunWith(classOf[JUnitRunner])
class SeqDeserializerTest extends DeserializerTest {

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

// TODO: ArraySeq is an EvidenceIterableFactory
//  it should "deserialize a list into an ArraySeq" in {
//    val result = deserialize[mutable.ArraySeq[Int]](listJson)
//    result should equal (listScala)
//  }

  it should "deserialize a list into a LinearSeq" in {
    val result = deserialize[LinearSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Seq" in {
    val result = deserialize[mutable.Seq[Int]](listJson)
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
