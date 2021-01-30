package com.fasterxml.jackson.module.scala

import java.io.{ByteArrayInputStream, File, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.{JsonMappingException, Module, ObjectMapper}
import com.fasterxml.jackson.module.scala.deser.OptionDeserializerTest.{Foo, Wrapper}

import scala.collection.JavaConverters._

object ClassTagExtensionsTest {
  private class PublicView
  private class PrivateView extends PublicView

  private trait SuperType {
    def getFoo: String
  }
  private case class SubType(foo: String, bar: Int) extends SuperType {
    def getFoo: String = foo
  }

  private object Target {
    def apply(foo: String, bar: Int): Target = {
      val result = new Target()
      result.foo = foo
      result.bar = bar
      result
    }
  }
  private class Target {
    @JsonView(Array(classOf[PublicView])) var foo: String = null
    @JsonView(Array(classOf[PrivateView])) var bar: Int = 0

    override def equals(p1: Any) = p1 match {
      case o: Target => foo == o.foo && bar == o.bar
      case _ => false
    }
  }

  private class Mixin(val foo: String)
  private case class GenericTestClass[T](t: T)
}

class ClassTagExtensionsTest extends JacksonTest {

  import ClassTagExtensionsTest._

  def module: Module = DefaultScalaModule
  val mapper = newMapperWithClassTagExtensions

  "An ObjectMapper with the ClassTagExtensions mixin" should "add mixin annotations" in {
    mapper.addMixin[Target, Mixin]()
    val result = mapper.findMixInClassFor[Target]
    result should equal(classOf[Mixin])
    val json = """{"foo":"value"}"""
    mapper.readValue[Target](json) shouldEqual new Target {
      foo = "value"
    }
  }

  it should "read value from json parser" in {
    val parser = mapper.getFactory.createParser(genericJson)
    val result = mapper.readValue[GenericTestClass[Int]](parser)
    result should equal(genericInt)
  }

  it should "read values from json parser" in {
    val parser = mapper.getFactory.createParser(listGenericJson)
    val result = mapper.readValues[GenericTestClass[Int]](parser).asScala.toList
    result should equal(listGenericInt)
  }

  it should "read value from tree node" in {
    val treeNode = mapper.readTree(genericJson).asInstanceOf[TreeNode]
    val result = mapper.treeToValue[GenericTestClass[Int]](treeNode)
    result should equal(genericInt)
  }

  it should "read value from file" in {
    withFile(genericJson) { file =>
      val result = mapper.readValue[GenericTestClass[Int]](file)
      result should equal(genericInt)
    }
  }

  it should "read value from URL" in {
    withFile(genericJson) { file =>
      val result = mapper.readValue[GenericTestClass[Int]](file.toURI.toURL)
      result should equal(genericInt)
    }
  }

  it should "read value from string" in {
    val result = mapper.readValue[GenericTestClass[Int]](genericJson)
    result should equal(genericInt)
  }

  it should "read value from Reader" in {
    val reader = new InputStreamReader(new ByteArrayInputStream(genericJson.getBytes))
    val result = mapper.readValue[GenericTestClass[Int]](reader)
    result should equal(genericInt)
  }

  it should "read value from stream" in {
    val stream = new ByteArrayInputStream(genericJson.getBytes)
    val result = mapper.readValue[GenericTestClass[Int]](stream)
    result should equal(genericInt)
  }

  it should "read value from byte array" in {
    val result = mapper.readValue[GenericTestClass[Int]](genericJson.getBytes)
    result should equal(genericInt)
  }

  it should "read value from subset of byte array" in {
    val result = mapper.readValue[GenericTestClass[Int]](genericJson.getBytes, 0, genericJson.length)
    result should equal(genericInt)
  }

  it should "produce writer with view" in {
    val instance = Target("foo", 42)
    val result = mapper.writerWithView[PublicView].writeValueAsString(instance)
    result should equal("""{"foo":"foo"}""")
    val resultInView = mapper.writerWithView[PrivateView].writeValueAsString(instance)
    resultInView should equal("""{"foo":"foo","bar":42}""")
  }

  it should "produce writer with type" in {
    val result = mapper.writerFor[SuperType].writeValueAsString(SubType("foo", 42))
    result should equal("""{"foo":"foo"}""")
  }

  it should "produce reader with type" in {
    val result = mapper.readerFor[GenericTestClass[Int]].readValue(genericJson).asInstanceOf[GenericTestClass[Int]]
    result should equal(genericInt)
  }

  it should "produce reader with view" in {
    val reader = mapper.readerWithView[PublicView].forType(classOf[Target])
    val result = reader.readValue("""{"foo":"foo","bar":42}""").asInstanceOf[Target]
    result should equal(Target.apply("foo", 0))
  }

  it should "convert between types" ignore {
    val result = mapper.convertValue[GenericTestClass[Int]](GenericTestClass("42"))
    result should equal(genericInt)
  }

  it should "read values as Array from a JSON array" in {
    val result = mapper.readValue[Array[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt.toArray)
  }

  it should "read values as Seq from a JSON array" ignore {
    val result = mapper.readValue[Seq[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "read values as List from a JSON array" ignore {
    val result = mapper.readValue[List[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "read values as Set from a JSON array" ignore {
    val result = mapper.readValue[Set[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt.toSet)
  }

  it should "fail to read as List from a non-Array JSON input" in {
    a [JsonMappingException] should be thrownBy {
      mapper.readValue[List[GenericTestClass[Int]]](genericJson)
    }
  }

  it should "read values as Map from a JSON object" in {
    val result = mapper.readValue[Map[String, String]](genericTwoFieldJson)
    result should equal(Map("first" -> "firstVal", "second" -> "secondVal"))
  }

  it should "read values as Map from a heterogeneous JSON object" in {
    val result = mapper.readValue[Map[String, Any]](genericMixedFieldJson)
    result should equal(Map("first" -> "firstVal", "second" -> 2))
  }

  it should "fail to read a Map from JSON with invalid types" ignore {
    an [InvalidFormatException] should be thrownBy {
      mapper.readValue[Map[String, Int]](genericTwoFieldJson)
    }
  }

  it should "read a generic Object from a JSON object" in {
    val result = mapper.readValue[Object](genericTwoFieldJson)
    assert(result.isInstanceOf[collection.Map[_, _]])
  }

  it should "read option values into List from a JSON array" ignore {
    val result = mapper.readValue[java.util.ArrayList[Option[String]]](toplevelOptionArrayJson).asScala
    result(0) should equal(Some("some"))
    result(1) should equal(None)
  }

  it should "read option values into Array from a JSON array" in {
    val result = mapper.readValue[Array[Option[String]]](toplevelOptionArrayJson)
    result(0) should equal(Some("some"))
    result(1) should equal(None)
  }

  it should "update value from file" ignore {
    withFile(toplevelArrayJson) { file =>
      val result = mapper.updateValue(List.empty[GenericTestClass[Int]], file)
      result should equal(listGenericInt)
    }
  }

  it should "update value from URL" ignore {
    withFile(toplevelArrayJson) { file =>
      val result = mapper.updateValue(List.empty[GenericTestClass[Int]], file.toURI.toURL)
      result should equal(listGenericInt)
    }
  }

  it should "update value from string" ignore {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "update value from Reader" ignore {
    val reader = new InputStreamReader(new ByteArrayInputStream(toplevelArrayJson.getBytes))
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], reader)
    result should equal(listGenericInt)
  }

  it should "update value from stream" ignore {
    val stream = new ByteArrayInputStream(toplevelArrayJson.getBytes)
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], stream)
    result should equal(listGenericInt)
  }

  it should "update value from byte array" ignore {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson.getBytes)
    result should equal(listGenericInt)
  }

  it should "update value from subset of byte array" ignore {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson.getBytes, 0, toplevelArrayJson.length)
    result should equal(listGenericInt)
  }

  it should "deserialize a type param wrapped option" ignore {
    val json: String = """{"t": {"bar": "baz"}}"""
    val result = mapper.readValue[Wrapper[Option[Foo]]](json)
    result.t.get.isInstanceOf[Foo] should be(true)
  }

  private val genericJson = """{"t":42}"""
  private val genericInt = GenericTestClass(42)
  private val listGenericJson = """{"t":42}{"t":31}"""
  private val listGenericInt = List(GenericTestClass(42), GenericTestClass(31))
  private val genericTwoFieldJson = """{"first":"firstVal","second":"secondVal"}"""
  private val genericMixedFieldJson = """{"first":"firstVal","second":2}"""
  private val toplevelArrayJson = """[{"t":42},{"t":31}]"""
  private val toplevelOptionArrayJson = """["some",null]"""

  private def withFile[T](contents: String)(body: File => T): T = {
    val file = File.createTempFile("jackson_scala_test", getClass.getSimpleName)
    try {
      Files.write(file.toPath, contents.getBytes(StandardCharsets.UTF_8))
      body(file)
    } finally {
      try {
        file.delete()
      } catch {
        case _: Exception => // ignore
      }
    }
  }

  private def newMapperWithClassTagExtensions: ObjectMapper with ClassTagExtensions = {
    newBuilder.build() :: ClassTagExtensions
  }
}
