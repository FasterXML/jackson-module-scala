package com.fasterxml.jackson.module.scala.ser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.map.JsonMappingException
import scala.collection.{mutable, immutable, Iterator}

/**
 * Undocumented class.
 */
@RunWith(classOf[JUnitRunner])
class IterableSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new JacksonModule with IterableSerializerModule

  "An ObjectMapper with IterableSerializer" should "serialize an Iterable[Int]" in {
    val iterable = new Iterable[Int] {
          def iterator = Iterator(1, 2, 3)
      }

    serialize(iterable) should be ("[1,2,3]")
  }

  it should "serialize a Seq[Int]" in {
    serialize(Seq(1,2,3)) should be ("[1,2,3]")
  }

  it should "serialize an immutable Set[Int]" in {
    serialize(immutable.Set(1,2,3)) should (matchUnorderedSet)
  }

  it should "serialize an immutable HashSet[Int]" in {
    serialize(immutable.HashSet(1,2,3)) should (matchUnorderedSet)
  }

  it should "serialize an immutable ListSet[Int]" in {
    serialize(immutable.ListSet(1,2,3)) should (matchUnorderedSet)
  }

  it should "serialize a mutable Set[Int]" in {
    serialize(mutable.Set(1,2,3)) should matchUnorderedSet
  }
  
  it should "serialize a mutable HashSet[Int]" in {
    serialize(mutable.HashSet(1,2,3)) should matchUnorderedSet
  }

  it should "serialize a mutable LinkedHashSet[Int]" in {
    serialize(mutable.LinkedHashSet(1,2,3)) should matchUnorderedSet
  }

  it should "not serialize a Map[Int]" in {
    intercept[JsonMappingException] {
      serialize(Map(1->2,3->4))
    }
  }

  val matchUnorderedSet = {
    be ("[1,2,3]") or
    be ("[1,3,2]") or
    be ("[2,1,3]") or
    be ("[2,3,1]") or
    be ("[3,1,2]") or
    be ("[3,2,1]")
  }
}