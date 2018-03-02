package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.scala.JacksonModule
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._
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

  it should "deserialize a list into a ResizableArray" in {
    val result = deserialize[mutable.ResizableArray[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserialize[mutable.ArraySeq[Int]](listJson)
    result should equal (listScala)
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

  it should "keep path index if error happened" in {
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      deserialize[List[UUID]](uuidListJson)
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal (List(1))
  }

  val listJson =  "[1,2,3,4,5,6]"
  val listScala = (1 to 6)
}
