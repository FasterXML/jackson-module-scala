package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.json.JsonMapper
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
    val result = deserialize[immutable.Iterable[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Iterable" in {
    val result = deserialize[mutable.Iterable[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an IndexedSeq" in {
    val result = deserialize[IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable IndexedSeq" in {
    val result = deserialize[immutable.IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable IndexedSeq" in {
    val result = deserialize[mutable.IndexedSeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a LazyList" in {
    import overrides._
    val result = deserialize[LazyList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LazyList" in {
    import overrides._
    val result = deserialize[LazyList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LinearSeq" in {
    val result = deserialize[immutable.LinearSeq[Int]](listJson)
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

  it should "deserialize a list into an immutable List" in {
    val result = deserialize[immutable.List[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Queue" in {
    val result = deserialize[immutable.Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Queue" in {
    val result = deserialize[mutable.Queue[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Stream" in {
    val result = deserialize[Stream[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Stream" in {
    val result = deserialize[immutable.Stream[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Seq" in {
    val result = deserialize[Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Seq" in {
    val result = deserialize[immutable.Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Seq" in {
    val result = deserialize[mutable.Seq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a Vector" in {
    val result = deserialize[Vector[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Vector" in {
    val result = deserialize[immutable.Vector[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayBuffer" in {
    val result = deserialize[mutable.ArrayBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayDeque" in {
    import overrides._
    val result = deserialize[ArrayDeque[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Buffer" in {
    val result = deserialize[mutable.Buffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ListBuffer" in {
    val result = deserialize[mutable.ListBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Stack" in {
    val result = deserialize[mutable.Stack[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable MutableList" in {
    import overrides._
    val result = deserialize[MutableList[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ResizableArray" in {
    import overrides._
    val result = deserialize[ResizableArray[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserialize[mutable.ArraySeq[Int]](listJson)
    result should equal (listScala)
  }

  it should "deserialize a list into an UnrolledBuffer" in {
    val result = deserialize[mutable.UnrolledBuffer[Int]](listJson)
    result should equal (listScala)
  }

  it should "keep path index if error happened" in {
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      deserialize[List[UUID]](uuidListJson)
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal (List(1))
  }

  it should "handle conversion of null to empty collection" in {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .changeDefaultNullHandling(_ => JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
      .build()
    val json = """{"s": null}"""
    val result1 = mapper.readValue(json, classOf[JavaListWrapper])
    result1 shouldEqual JavaListWrapper(new java.util.ArrayList[String]())
    val result2 = mapper.readValue(json, classOf[SeqWrapper])
    result2 shouldEqual SeqWrapper(Seq.empty)
  }

  val listJson =  "[1,2,3,4,5,6]"
  val listScala: Range.Inclusive = 1 to 6
}
