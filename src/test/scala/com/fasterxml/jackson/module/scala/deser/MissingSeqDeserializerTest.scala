package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner

import collection.LinearSeq
import collection.mutable
import collection.immutable.Queue
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

object MissingSeqDeserializerTest {
  private case class TestClass[T](field: T)
}

@RunWith(classOf[JUnitRunner])
class MissingSeqDeserializerTest extends DeserializerTest {

  import MissingSeqDeserializerTest._

  lazy val module = DefaultScalaModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a missing list into a Seq" in {
    val result = deserialize[TestClass[collection.Seq[Int]]](emptyObject)
    result.field should equal (collection.Seq.empty)
  }

  it should "deserialize a missing list into an IndexedSeq" in {
    val result = deserialize[TestClass[IndexedSeq[Int]]](emptyObject)
    result.field should equal (IndexedSeq.empty)
  }

  it should "deserialize a missing list into a mutable IndexedSeq" in {
    val result = deserialize[TestClass[mutable.IndexedSeq[Int]]](emptyObject)
    result.field should equal (mutable.IndexedSeq.empty)
  }

  it should "deserialize a missing list into a Vector" in {
    val result = deserialize[TestClass[Vector[Int]]](emptyObject)
    result.field should equal (Vector.empty)
  }

  it should "deserialize a missing list into a ResizableArray" in {
    val result = deserialize[TestClass[mutable.ResizableArray[Int]]](emptyObject)
    result.field should equal (mutable.ResizableArray.empty)
  }

  it should "deserialize a missing list into an ArraySeq" in {
    val result = deserialize[TestClass[mutable.ArraySeq[Int]]](emptyObject)
    result.field should equal (mutable.ArraySeq.empty)
  }

  it should "deserialize a missing list into a LinearSeq" in {
    val result = deserialize[TestClass[LinearSeq[Int]]](emptyObject)
    result.field should equal (LinearSeq.empty)
  }

  it should "deserialize a missing list into a mutable LinearSeq" in {
    val result = deserialize[TestClass[mutable.LinearSeq[Int]]](emptyObject)
    result.field should equal (mutable.LinearSeq.empty)
  }

  it should "deserialize a missing list into a List" in {
    val result = deserialize[TestClass[List[Int]]](emptyObject)
    result.field should equal (List.empty)
  }

  it should "deserialize a missing list into a Stream" in {
    val result = deserialize[TestClass[Stream[Int]]](emptyObject)
    result.field should equal (Stream.empty)
  }

  it should "deserialize a missing list into a MutableList" in {
    val result = deserialize[TestClass[mutable.MutableList[Int]]](emptyObject)
    result.field should equal (mutable.MutableList.empty)
  }

  it should "deserialize a missing list into an immutable Queue" in {
    val result = deserialize[TestClass[Queue[Int]]](emptyObject)
    result.field should equal (Queue.empty)
  }

  it should "deserialize a missing list into a mutable Queue" in {
    val result = deserialize[TestClass[mutable.Queue[Int]]](emptyObject)
    result.field should equal (mutable.Queue.empty)
  }

  it should "deserialize a missing list into a mutable Buffer" in {
    val result = deserialize[TestClass[mutable.Buffer[Int]]](emptyObject)
    result.field should equal (mutable.Buffer.empty)
  }

  it should "deserialize a missing list into a mutable ArrayBuffer" in {
    val result = deserialize[TestClass[mutable.ArrayBuffer[Int]]](emptyObject)
    result.field should equal (mutable.ArrayBuffer.empty)
  }

  it should "deserialize a missing list into a mutable ListBuffer" in {
    val result = deserialize[TestClass[mutable.ListBuffer[Int]]](emptyObject)
    result.field should equal (mutable.ListBuffer.empty)
  }

  val emptyObject =  "{}"
}