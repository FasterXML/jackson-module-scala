package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty, JsonTypeInfo}
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.scalatest.matchers.Matcher

import scala.collection.{Iterator, immutable, mutable}

class NonEmptyCollections {
  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def emptyIterable: Iterable[Int] = Iterable.empty[Int]

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  def nonEmptyIterable = Iterable(1, 2, 3)
}

object IterableSerializerTest {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
  trait C

  case class X(data: String) extends C

  case class Y(data: Int) extends C

  case class CHolder(c: Seq[C])

}

class IterableSerializerTest extends SerializerTest {

  import IterableSerializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with IterableSerializer" should "serialize an Iterable[Int]" in {
    val iterable = new Iterable[Int] {
      def iterator = Iterator(1, 2, 3)
    }

    serialize(iterable) should be("[1,2,3]")
  }

  it should "serialize a Seq[Int]" in {
    serialize(Seq(1, 2, 3)) should be("[1,2,3]")
  }

  it should "serialize a Array[Int]" in {
    serialize(Array(1, 2, 3)) should be("[1,2,3]")
  }

  it should "serialize an immutable Set[Int]" in {
    serialize(immutable.Set(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize an immutable HashSet[Int]" in {
    serialize(immutable.HashSet(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize an immutable ListSet[Int]" in {
    serialize(immutable.ListSet(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize a mutable Set[Int]" in {
    serialize(mutable.Set(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize a mutable HashSet[Int]" in {
    serialize(mutable.HashSet(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize a mutable LinkedHashSet[Int]" in {
    serialize(mutable.LinkedHashSet(1, 2, 3)) should matchUnorderedSet
  }

  it should "serialize a Map[Int]" in {
    serialize(Map(1 -> 2, 3 -> 4)) should matchUnorderedMap
  }

  it should "honor the JsonInclude(NON_EMPTY) annotation" in {
    serialize(new NonEmptyCollections) should be("""{"nonEmptyIterable":[1,2,3]}""")
  }

  it should "honor JsonTypeInfo" in {
    serialize(CHolder(Seq[C](X("1"), X("2")))) shouldBe """{"c":[{"@class":"com.fasterxml.jackson.module.scala.ser.IterableSerializerTest$X","data":"1"},{"@class":"com.fasterxml.jackson.module.scala.ser.IterableSerializerTest$X","data":"2"}]}"""
  }

  it should "honor SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED" in {
    val mapper = newBuilder.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
      .build()
    mapper.writeValueAsString(Seq("123")) shouldBe """"123""""
    mapper.writeValueAsString(Seq("123", "abc")) shouldBe """["123","abc"]"""
  }

  val matchUnorderedSet: Matcher[Any] = {
    be("[1,2,3]") or
      be("[1,3,2]") or
      be("[2,1,3]") or
      be("[2,3,1]") or
      be("[3,1,2]") or
      be("[3,2,1]")
  }

  val matchUnorderedMap: Matcher[Any] = {
    be("{\"1\":2,\"3\":4}") or
      be("{\"3\":4,\"1\":2}")
  }
}
