package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.{JsonSetter, Nulls}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import java.util.UUID
import scala.collection.{immutable, mutable}

object SeqDeserializerTest {
  case class JavaListWrapper(s: java.util.ArrayList[String])
  case class SeqWrapper(s: Seq[String])

  case class SeqOptionString(values: Seq[Option[String]])
  case class WrappedSeqOptionString(text: String, wrappedStrings: SeqOptionString)

  case class SeqOptionLong(values: Seq[Option[Long]])
  case class WrappedSeqOptionLong(text: String, wrappedLongs: SeqOptionLong)
}

class SeqDeserializerTest extends DeserializerTest {

  import SeqDeserializerTest._
  lazy val module = new JacksonModule with SeqDeserializerModule

  "An ObjectMapper with the SeqDeserializer" should "deserialize a list into an Iterable" in {
    val result = deserialize(listJson, classOf[Iterable[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Iterable" in {
    val result = deserialize(listJson, classOf[immutable.Iterable[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Iterable" in {
    val result = deserialize(listJson, classOf[mutable.Iterable[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an IndexedSeq" in {
    val result = deserialize(listJson, classOf[IndexedSeq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable IndexedSeq" in {
    val result = deserialize(listJson, classOf[immutable.IndexedSeq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable IndexedSeq" in {
    val result = deserialize(listJson, classOf[mutable.IndexedSeq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Array" in {
    val result = deserialize(listJson, classOf[Array[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LazyList" in {
    import overrides._
    val result = deserialize(listJson, classOf[LazyList[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable LinearSeq" in {
    val result = deserialize(listJson, classOf[immutable.LinearSeq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable LinearSeq" in {
    val result = deserialize(listJson, new TypeReference[mutable.LinearSeq[Int]] {})
    result should equal (listScala)
  }

  it should "deserialize a list into a List" in {
    val result = deserialize(listJson, classOf[List[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable List" in {
    val result = deserialize(listJson, classOf[immutable.List[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Queue" in {
    val result = deserialize(listJson, classOf[immutable.Queue[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Queue" in {
    val result = deserialize(listJson, classOf[mutable.Queue[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a Stream" in {
    val result = deserialize(listJson, classOf[Stream[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Stream" in {
    val result = deserialize(listJson, classOf[immutable.Stream[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a Seq" in {
    val result = deserialize(listJson, classOf[Seq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Seq" in {
    val result = deserialize(listJson, classOf[immutable.Seq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Seq" in {
    val result = deserialize(listJson, classOf[mutable.Seq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a Vector" in {
    val result = deserialize(listJson, classOf[Vector[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an immutable Vector" in {
    val result = deserialize(listJson, classOf[immutable.Vector[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayBuffer" in {
    val result = deserialize(listJson, classOf[mutable.ArrayBuffer[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ArrayDeque" in {
    import overrides._
    val result = deserialize(listJson, classOf[ArrayDeque[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Buffer" in {
    val result = deserialize(listJson, classOf[mutable.Buffer[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ListBuffer" in {
    val result = deserialize(listJson, classOf[mutable.ListBuffer[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable Stack" in {
    val result = deserialize(listJson, classOf[mutable.Stack[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable MutableList" in {
    import overrides._
    val result = deserialize(listJson, classOf[MutableList[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into a mutable ResizableArray" in {
    import overrides._
    val result = deserialize(listJson, classOf[ResizableArray[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an ArraySeq" in {
    val result = deserialize(listJson, classOf[mutable.ArraySeq[Int]])
    result should equal (listScala)
  }

  it should "deserialize a list into an UnrolledBuffer" in {
    val result = deserialize(listJson, classOf[mutable.UnrolledBuffer[Int]])
    result should equal (listScala)
  }

  it should "keep path index if error happened" in {
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      deserialize(uuidListJson, classOf[List[UUID]], classOf[UUID])
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal (List(1))
  }

  it should "deserialize a nested seq into an immutable Seq" in {
    val result = deserialize(nestedJson, classOf[immutable.Seq[Seq[Int]]], classOf[Seq[Int]])
    result shouldEqual Seq(Seq(1,2,3),Seq(4,5,6))
  }

  it should "deserialize a seq of options" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val s1 = Seq(Some("string1"), Some("string2"), None)
    val t1 = mapper.writeValueAsString(s1)
    val v1 = mapper.readValue(t1, new TypeReference[Seq[Option[String]]]{})
    v1 shouldEqual s1
  }

  it should "deserialize case class with a seq of options" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val s1 = SeqOptionString(Seq(Some("string1"), Some("string2"), None))
    val t1 = mapper.writeValueAsString(s1)
    val v1 = mapper.readValue(t1, classOf[SeqOptionString])
    v1 shouldEqual s1
  }

  it should "deserialize case class nested with a seq of options" in {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    val w1 = WrappedSeqOptionString("myText", SeqOptionString(Seq(Some("string1"), Some("string2"), None)))
    val t1 = mapper.writeValueAsString(w1)
    val v1 = mapper.readValue(t1, classOf[WrappedSeqOptionString])
    v1 shouldEqual w1
  }

  it should "deserialize case class with a seq of longs (when ScalaAnnotationIntrospector register is used)" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[SeqOptionLong], "values", classOf[Long])
    try {
      val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
      val w1 = WrappedSeqOptionLong("myText", SeqOptionLong(Seq(Some(100L), Some(100000000000000L), None)))
      val t1 = mapper.writeValueAsString(w1)
      val v1 = mapper.readValue(t1, classOf[WrappedSeqOptionLong])
      v1 shouldEqual w1
      v1.wrappedLongs.values.map(useOptionLong).sum shouldEqual w1.wrappedLongs.values.map(useOptionLong).sum
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
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

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)
}
