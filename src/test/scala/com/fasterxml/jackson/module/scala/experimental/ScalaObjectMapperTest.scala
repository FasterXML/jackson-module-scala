package com.fasterxml.jackson.module.scala.experimental

import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import java.io.{ByteArrayInputStream, InputStreamReader}
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.exc.InvalidFormatException

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

private case class ListContainerWithSomeDefault(xs: List[String] = List("a", "b", "c"))
private case class ListContainerWithNilAsDefault(xs: List[Int] = Nil)
private case class MultiValuedListContainerDefaults(xs: List[Any] = Nil, ys: List[Boolean] = List(false, true, true))

@RunWith(classOf[JUnitRunner])
class ScalaObjectMapperTest extends FlatSpec with Matchers {

  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  "An ObjectMapper with the ScalaObjectMapper mixin" should "add mixin annotations" in {
    mapper.addMixInAnnotations[Target, Mixin]()
    val result = mapper.findMixInClassFor[Target]
    result should equal(classOf[Mixin])
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
    import scala.collection.JavaConversions._
    val parser = mapper.getFactory.createParser(listGenericJson)
    val result = mapper.readValues[GenericTestClass[Int]](parser).toList
    result should equal(listGenericInt)
  }

  it should "read value from tree node" in {
    val treeNode = mapper.readTree(genericJson).asInstanceOf[TreeNode]
    val result = mapper.treeToValue[GenericTestClass[Int]](treeNode)
    result should equal(genericInt)
  }

  it should "know if it can serialize a seralizable type" in {
    val result = mapper.canSerialize[Target]
    result should equal(mapper.canSerialize(classOf[Target]))
  }

  it should "know if it can deserialize a deserializable type" in {
    val result = mapper.canDeserialize[Target]
    result should equal(mapper.canDeserialize(mapper.constructType(classOf[Target])))
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
    val result = mapper.writerWithType[SuperType].writeValueAsString(SubType("foo", 42))
    result should equal("""{"foo":"foo"}""")
  }

  it should "produce reader with type" in {
    val result = mapper.reader[GenericTestClass[Int]].readValue(genericJson).asInstanceOf[GenericTestClass[Int]]
    result should equal(genericInt)
  }

  it should "produce reader with view" in {
    val reader = mapper.readerWithView[PublicView].withType(classOf[Target])
    val result = reader.readValue("""{"foo":"foo","bar":42}""").asInstanceOf[Target]
    result should equal(Target.apply("foo", 0))
  }

  it should "convert between types" in {
    val result = mapper.convertValue[GenericTestClass[Int]](GenericTestClass("42"))
    result should equal(genericInt)
  }

  it should "generate json schema" in {
    val result = mapper.generateJsonSchema[Target]
    result should equal(mapper.generateJsonSchema(classOf[Target]))
  }

  it should "read values as Array from a JSON array" in {
    val result = mapper.readValue[Array[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt.toArray)
  }

  it should "read values as Seq from a JSON array" in {
    val result = mapper.readValue[Seq[GenericTestClass[Int]]](toplevelArrayJson)
    result should equal(listGenericInt.toSeq)
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

  it should "read a option values from a JSON array" in {
    val result = mapper.readValue[List[Option[String]]](toplevelOptionArrayJson)
    result(0) should equal(Some("some"))
    result(1) should equal(None)
  }

  it should "use default values from constructor for empty array fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithSomeDefault](emptyArrayFieldJson)
    result should equal (ListContainerWithSomeDefault(Nil))
  }

  it should "use default values from constructor for nullifed array fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithSomeDefault](nullifiedArrayFieldJson)
    result should equal (ListContainerWithSomeDefault(List("a", "b", "c")))
  }

  it should "ignore default values for non-empty array type fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithSomeDefault](stringArrayFieldJson)
    result should equal (ListContainerWithSomeDefault(List("x", "y", "z")))
  }

  it should "use Nil from constructor for nullified array fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithNilAsDefault](nullifiedArrayFieldJson)
    result should equal (ListContainerWithNilAsDefault(Nil))
  }

  it should "use Nil from constructor for empty array fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithNilAsDefault](emptyArrayFieldJson)
    result should equal (ListContainerWithNilAsDefault(Nil))
  }

  it should "ignore defaulted Nil for non-empty array type fields" in {
    val result = mapper.readValueWithDefaults[ListContainerWithNilAsDefault](intArrayFieldJson)
    result should equal (ListContainerWithNilAsDefault(List(1, 2, 3)))
  }

  it should "return defaults if the property is absent" in {
    val result = mapper.readValueWithDefaults[ListContainerWithSomeDefault](emptyJson)
    result should equal (ListContainerWithSomeDefault(List("a", "b", "c")))
  }

  it should "use multiple default parameters from constructor for array typed fields" in {
    val result = mapper.readValueWithDefaults[MultiValuedListContainerDefaults](multipleArrayFieldsJson)
    result should equal (MultiValuedListContainerDefaults(List("x", "y", "z"), List()))
  }

  // No tests for the following functions:
  //  def readValue[T: Manifest](src: File): T
  //  def readValue[T: Manifest](src: URL): T
  //  def acceptJsonFormatVisitor[T: Manifest](visitor: JsonFormatVisitorWrapper): Unit

  private val genericJson = """{"t":42}"""
  private val genericInt = GenericTestClass(42)
  private val listGenericJson = """{"t":42}{"t":31}"""
  private val listGenericInt = List(GenericTestClass(42), GenericTestClass(31))
  private val genericTwoFieldJson = """{"first":"firstVal","second":"secondVal"}"""
  private val genericMixedFieldJson = """{"first":"firstVal","second":2}"""
  private val toplevelArrayJson = """[{"t":42},{"t":31}]"""
  private val toplevelOptionArrayJson = """["some",null]"""
  private val emptyArrayFieldJson = """{"xs": []}"""
  private val nullifiedArrayFieldJson = """{"xs": null}"""
  private val stringArrayFieldJson = """{"xs": ["x", "y", "z"]}"""
  private val intArrayFieldJson = """{"xs": [1, 2, 3]}"""
  private val emptyJson = """{}"""
  private val multipleArrayFieldsJson = """{"xs": ["x", "y", "z"], "ys":[]}"""
}
