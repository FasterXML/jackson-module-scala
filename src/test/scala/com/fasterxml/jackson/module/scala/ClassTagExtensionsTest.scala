package com.fasterxml.jackson.module.scala

import java.io.{ByteArrayInputStream, File, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.{JsonMappingException, Module, ObjectMapper}
import com.fasterxml.jackson.module.scala.deser.IntMapDeserializerTest.IntMapWrapper
import com.fasterxml.jackson.module.scala.deser.OptionDeserializerTest.{Foo, TWrapper}
import com.fasterxml.jackson.module.scala.deser.OptionWithNumberDeserializerTest.{OptionLong, WrappedOptionLong}
import com.fasterxml.jackson.module.scala.deser.SeqDeserializerTest.{SeqOptionString, WrappedSeqOptionString}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.collection.immutable.IntMap

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

  class MapWrapper {
    val stringLongMap = Map[String, Long]("1" -> 11L, "2" -> 22L)
  }

  class AnnotatedMapWrapper {
    @JsonDeserialize(contentAs = classOf[Long])
    val stringLongMap = Map[String, Long]("1" -> 11L, "2" -> 22L)
  }
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

  it should "construct the proper java type" in {
    val result = mapper.constructType[Target]
    result should equal(mapper.constructType(classOf[Target]))
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
    val reader = new InputStreamReader(new ByteArrayInputStream(genericJson.getBytes(StandardCharsets.UTF_8)))
    val result = mapper.readValue[GenericTestClass[Int]](reader)
    result should equal(genericInt)
  }

  it should "read value from stream" in {
    val stream = new ByteArrayInputStream(genericJson.getBytes(StandardCharsets.UTF_8))
    val result = mapper.readValue[GenericTestClass[Int]](stream)
    result should equal(genericInt)
  }

  it should "read value from byte array" in {
    val result = mapper.readValue[GenericTestClass[Int]](genericJson.getBytes(StandardCharsets.UTF_8))
    result should equal(genericInt)
  }

  it should "read value from subset of byte array" in {
    val result = mapper.readValue[GenericTestClass[Int]](genericJson.getBytes(StandardCharsets.UTF_8), 0, genericJson.length)
    result should equal(genericInt)
  }

  //https://github.com/FasterXML/jackson-core/issues/755
  it should "deserialize Array[Float]" in {
    val sb = new StringBuilder
    sb.append('[')
      .append("\"7.038531e-26\",")
      .append("\"1.199999988079071\",")
      .append("\"3.4028235677973366e38\",")
      .append("\"7.006492321624086e-46\"")
      .append(']')
    val floats = mapper.readValue[Array[Float]](sb.toString)
    floats should  have length 4
    floats(0).toString shouldEqual "7.038531E-26" //toString needed in scala 2.11, assert fails otherwise
    floats(1) shouldEqual 1.1999999f
    floats(2).toString shouldEqual "3.4028235E38" //toString needed in scala 2.11, won't compile otherwise
    floats(3).toString shouldEqual "1.4E-45" //this assertion fails unless toString is used
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

  it should "convert between types" in {
    val result = mapper.convertValue[GenericTestClass[Int]](GenericTestClass("42"))
    result should equal(genericInt)
  }

  it should "read values as Array from a JSON array" in {
    val result = mapper.readValue[Array[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt.toArray)
  }

  it should "read values as Seq from a JSON array" in {
    val result = mapper.readValue[Seq[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "read values as List from a JSON array" in {
    val result = mapper.readValue[List[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "read values as Set from a JSON array" in {
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

  it should "fail to read a Map from JSON with invalid types" in {
    an [InvalidFormatException] should be thrownBy {
      mapper.readValue[Map[String, Int]](genericTwoFieldJson)
    }
  }

  it should "read a generic Object from a JSON object" in {
    val result = mapper.readValue[Object](genericTwoFieldJson)
    assert(result.isInstanceOf[collection.Map[_, _]])
  }

  it should "read option values into List from a JSON array" in {
    val result = mapper.readValue[java.util.ArrayList[Option[String]]](toplevelOptionArrayJson).asScala
    result(0) should equal(Some("some"))
    result(1) should equal(None)
  }

  it should "read option values into Array from a JSON array" in {
    val result = mapper.readValue[Array[Option[String]]](toplevelOptionArrayJson)
    result(0) should equal(Some("some"))
    result(1) should equal(None)
  }

  it should "update value from file" in {
    withFile(toplevelArrayJson) { file =>
      val result = mapper.updateValue(List.empty[GenericTestClass[Int]], file)
      result should equal(listGenericInt)
    }
  }

  it should "update value from URL" in {
    withFile(toplevelArrayJson) { file =>
      val result = mapper.updateValue(List.empty[GenericTestClass[Int]], file.toURI.toURL)
      result should equal(listGenericInt)
    }
  }

  it should "update value from string" in {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson)
    result should equal(listGenericInt)
  }

  it should "update value from Reader" in {
    val reader = new InputStreamReader(new ByteArrayInputStream(toplevelArrayJson.getBytes(StandardCharsets.UTF_8)))
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], reader)
    result should equal(listGenericInt)
  }

  it should "update value from stream" in {
    val stream = new ByteArrayInputStream(toplevelArrayJson.getBytes(StandardCharsets.UTF_8))
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], stream)
    result should equal(listGenericInt)
  }

  it should "update value from byte array" in {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson.getBytes(StandardCharsets.UTF_8))
    result should equal(listGenericInt)
  }

  it should "update value from subset of byte array" in {
    val result = mapper.updateValue(List.empty[GenericTestClass[Int]], toplevelArrayJson.getBytes(StandardCharsets.UTF_8), 0, toplevelArrayJson.length)
    result should equal(listGenericInt)
  }

  it should "deserialize a type param wrapped option" in {
    val json: String = """{"t": {"bar": "baz"}}"""
    val result = mapper.readValue[TWrapper[Option[Foo]]](json)
    result.t.get.isInstanceOf[Foo] should be(true)
  }

  //https://github.com/FasterXML/jackson-module-scala/issues/241 -- currently fails
  it should "deserialize MapWrapper" ignore {
    val mw = new MapWrapper
    val json = mapper.writeValueAsString(mw)
    val mm = mapper.readValue[MapWrapper](json)
    val result = mm.stringLongMap("1")
    result shouldEqual 11
  }

  it should "deserialize AnnotatedMapWrapper" in {
    val mw = new AnnotatedMapWrapper
    val json = mapper.writeValueAsString(mw)
    val mm = mapper.readValue[AnnotatedMapWrapper](json)
    val result = mm.stringLongMap("1")
    result shouldEqual 11
  }

  it should "deserialize WrappedOptionLong" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    try {
      val v1 = mapper.readValue[WrappedOptionLong]("""{"text":"myText","wrappedLong":{"valueLong":151}}""")
      v1 shouldBe WrappedOptionLong("myText", OptionLong(Some(151L)))
      v1.wrappedLong.valueLong.get shouldBe 151L
      //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in OptionWithNumberDeserializerTest
      useOptionLong(v1.wrappedLong.valueLong) shouldBe 302L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize a seq of options" in {
    val s1 = Seq(Some("string1"), Some("string2"), None)
    val t1 = mapper.writeValueAsString(s1)
    val v1 = mapper.readValue[Seq[Option[String]]](t1)
    v1 shouldEqual s1
  }

  it should "deserialize case class with a seq of options" in {
    val s1 = SeqOptionString(Seq(Some("string1"), Some("string2"), None))
    val t1 = mapper.writeValueAsString(s1)
    val v1 = mapper.readValue[SeqOptionString](t1)
    v1 shouldEqual s1
  }

  it should "deserialize case class nested with a seq of options" in {
    val w1 = WrappedSeqOptionString("myText", SeqOptionString(Seq(Some("string1"), Some("string2"), None)))
    val t1 = mapper.writeValueAsString(w1)
    val v1 = mapper.readValue[WrappedSeqOptionString](t1)
    v1 shouldEqual w1
  }

  it should "deserialize IntMap[Long]]" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)

    val json = mapper.writeValueAsString(map)
    val read = mapper.readValue[IntMap[Long]](json)

    read shouldEqual map
    read.values.sum shouldEqual map.values.sum
  }

  it should "deserialize IntMapWrapper" in {
    val map = IntMap(1 -> 100L, 2 -> 200L)
    val instance = IntMapWrapper(map)

    val json = mapper.writeValueAsString(instance)
    val read = mapper.readValue[IntMapWrapper](json)

    read shouldEqual instance
    //next line fails because ClassTagExtensions does not keep introspecting into the child fields
    //read.values.values.sum shouldEqual map.values.sum
  }

  "JavaTypeable" should "handle Option[Int]" in {
    val jt = implicitly[JavaTypeable[Option[Int]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[Option[_]]
    jt.containedType(0).getRawClass shouldEqual classOf[Int]
  }

  it should "handle Option[Boolean]" in {
    val jt = implicitly[JavaTypeable[Option[Boolean]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[Option[_]]
    jt.containedType(0).getRawClass shouldEqual classOf[Boolean]
  }

  it should "handle Seq[Int]" in {
    val jt = implicitly[JavaTypeable[Seq[Int]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[Seq[_]]
    jt.containedType(0).getRawClass shouldEqual classOf[Int]
  }

  it should "handle IntMap[Long]" in {
    val jt = implicitly[JavaTypeable[IntMap[Long]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[IntMap[_]]
    jt shouldBe a[MapLikeType]
    val mlt = jt.asInstanceOf[MapLikeType]
    mlt.getKeyType.getRawClass shouldEqual classOf[Int]
    mlt.getContentType.getRawClass shouldEqual classOf[Long]
  }

  it should "handle immutable LongMap[Boolean]" in {
    val jt = implicitly[JavaTypeable[immutable.LongMap[Boolean]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[immutable.LongMap[_]]
    jt shouldBe a[MapLikeType]
    val mlt = jt.asInstanceOf[MapLikeType]
    mlt.getKeyType.getRawClass shouldEqual classOf[Long]
    mlt.getContentType.getRawClass shouldEqual classOf[Boolean]
  }

  it should "handle mutable LongMap[BigInt]" in {
    val jt = implicitly[JavaTypeable[mutable.LongMap[BigInt]]].asJavaType(mapper.getTypeFactory)
    jt.getRawClass shouldEqual classOf[mutable.LongMap[_]]
    jt shouldBe a[MapLikeType]
    val mlt = jt.asInstanceOf[MapLikeType]
    mlt.getKeyType.getRawClass shouldEqual classOf[Long]
    mlt.getContentType.getRawClass shouldEqual classOf[BigInt]
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

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)
}
