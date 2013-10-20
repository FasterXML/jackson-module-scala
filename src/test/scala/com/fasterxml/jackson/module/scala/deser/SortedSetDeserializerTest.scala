package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable
import scala.collection.SortedSet
import scala.collection.immutable.TreeSet
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.core.`type`.TypeReference

object SortedSetDeserializerTest
{
  case class LongSetHolder(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: SortedSet[Long])

  case class ComparableBean(value: Long) extends Comparable[ComparableBean] {
    def compareTo(o: ComparableBean): Int = value compareTo o.value
  }
}

@RunWith(classOf[JUnitRunner])
class SortedSetDeserializerTest extends DeserializationFixture with ShouldMatchers {
  import SortedSetDeserializerTest._

  behavior of "SortedSetDeserializer"

  it should "deserialize a list into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[String]](setJson)
    result should be === setScala
  }

  // Not supported in 2.9; need to find a way to cross-version test
//  it should "deserialize a list into a mutable SortedSet" in { f =>
//    val result = f.readValue[mutable.SortedSet[String]](setJson)
//    result should be === setScala
//  }

  it should "deserialize a list into a TreeSet" in { f =>
    val result = f.readValue[TreeSet[String]](setJson)
    result should be === setScala
  }

  // Not supported in 2.9; need to find a way to cross-version test
//  it should "deserialize a list into a mutable TreeSet" in { f =>
//    val result = f.readValue[mutable.TreeSet[String]](setJson)
//    result should be === setScala
//  }

  it should "deserialize a list of Ints into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[Int]](intSetJson)
    result should be === intSetScala
  }

  it should "deserialize a list of Longs into a SortedSet" in { f =>
    val result = f.readValue[LongSetHolder](longSetHolderJson)
    result should be === longSetHolderScala
  }

  it should "deserialize a list of Comparables into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[ComparableBean]](comparableSetJson)
    result should be === comparableSetScala
  }

  it should "deserialize a lit of Ints into a SortedSet of Options" in { f =>
    // NB: This is `java.lang.Integer`, because of GH-104
    val result = f.readValue[SortedSet[Option[Integer]]](intSetJson, new TypeReference[SortedSet[Option[Integer]]]{})
    result should be === optionIntSetScala
  }

  val setJson = """[ "one", "two", "three" ]"""
  val setScala = SortedSet("one", "two", "three")
  val intSetJson = """[ 1, 2, 3 ]"""
  val intSetScala = SortedSet(3, 2, 1)
  val longSetHolderJson = """{"value":[1,2,3]}"""
  val longSetHolderScala = LongSetHolder(SortedSet(3,2,1))
  val comparableSetJson = """[{"value": 3},{"value": 1},{"value": 2}]"""
  val comparableSetScala = SortedSet(ComparableBean(2), ComparableBean(3), ComparableBean(1))
  val optionIntSetScala = SortedSet(Option(2), Option(3), Option(1))

}
