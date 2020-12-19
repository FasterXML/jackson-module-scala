package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.collection.{immutable, mutable}

case class JavaListWrapper(s: java.util.ArrayList[String])
case class SeqWrapper(s: Seq[String])

@RunWith(classOf[JUnitRunner])
class SeqDeserializerTest extends DeserializerTest {

  lazy val module = new JacksonModule with SeqDeserializerModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into an Iterable" in {
    val result = deserialize[Iterable[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Iterable" in {
    val result = deserializeWithManifest[immutable.Iterable[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Iterable" in {
    val result = deserializeWithManifest[mutable.Iterable[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an IndexedSeq" in {
    val result = deserializeWithManifest[IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable IndexedSeq" in {
    val result = deserializeWithManifest[immutable.IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable IndexedSeq" in {
    val result = deserializeWithManifest[mutable.IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a LazyList" in {
    import overrides._
    val result = deserializeWithManifest[LazyList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LazyList" in {
    import overrides._
    val result = deserializeWithManifest[LazyList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LinearSeq" in {
    val result = deserializeWithManifest[immutable.LinearSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable LinearSeq" in {
    val result = deserializeWithManifest[mutable.LinearSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a List" in {
    val result = deserializeWithManifest[List[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable List" in {
    val result = deserializeWithManifest[immutable.List[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Queue" in {
    val result = deserializeWithManifest[immutable.Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Queue" in {
    val result = deserializeWithManifest[mutable.Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Stream" in {
    val result = deserializeWithManifest[Stream[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Stream" in {
    val result = deserializeWithManifest[immutable.Stream[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Seq" in {
    val result = deserializeWithManifest[Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Seq" in {
    val result = deserializeWithManifest[immutable.Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Seq" in {
    val result = deserializeWithManifest[mutable.Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Vector" in {
    val result = deserializeWithManifest[Vector[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Vector" in {
    val result = deserializeWithManifest[immutable.Vector[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayBuffer" in {
    val result = deserializeWithManifest[mutable.ArrayBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayDeque" in {
    import overrides._
    val result = deserializeWithManifest[ArrayDeque[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Buffer" in {
    val result = deserializeWithManifest[mutable.Buffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ListBuffer" in {
    val result = deserializeWithManifest[mutable.ListBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Stack" in {
    val result = deserializeWithManifest[mutable.Stack[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable MutableList" in {
    import overrides._
    val result = deserializeWithManifest[MutableList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ResizableArray" in {
    import overrides._
    val result = deserializeWithManifest[ResizableArray[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserializeWithManifest[mutable.ArraySeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an UnrolledBuffer" in {
    val result = deserializeWithManifest[mutable.UnrolledBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "keep path index if error happened" in {
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      deserializeWithManifest[List[UUID]](uuidListJson)
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal (List(1))
  }

  it should "deserialize a nested seq into an immutable Seq" in {
    val result = deserializeWithManifest[immutable.Seq[Seq[Int]]](nestedJson)
    result shouldEqual Seq(Seq(1,2,3),Seq(4,5,6))
  }

  it should "handle AS_NULL" in {
    val mapper = new ObjectMapper
    mapper.registerModule(new DefaultScalaModule)
    mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
    val json = """{"s": null}"""
    val result1 = mapper.readValue(json, classOf[JavaListWrapper])
    result1 shouldEqual JavaListWrapper(new java.util.ArrayList[String]())
    val result2 = mapper.readValue(json, classOf[SeqWrapper])
    result2 shouldEqual SeqWrapper(Seq.empty)
  }

  val listJson =  "[1,2,3,4,5,6]"
  val listScala: Range.Inclusive = 1 to 6
  val nestedJson =  "[[1,2,3],[4,5,6]]"
}
