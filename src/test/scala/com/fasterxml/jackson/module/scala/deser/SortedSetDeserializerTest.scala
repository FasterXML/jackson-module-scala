package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection._

object SortedSetDeserializerTest {
  case class LongSetHolder(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: SortedSet[Long])

  case class ComparableBean(value: Long) extends Comparable[ComparableBean] {
    def compareTo(o: ComparableBean): Int = value compareTo o.value
  }
}

@RunWith(classOf[JUnitRunner])
class SortedSetDeserializerTest extends DeserializationFixture {
  import SortedSetDeserializerTest._

  behavior of "SortedSetDeserializer"

  it should "deserialize a list into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[String]](setJson)
    result shouldBe setScala
  }

  it should "deserialize a list into an immutable SortedSet" in { f =>
    val result = f.readValue[immutable.SortedSet[String]](setJson)
    result shouldBe setScala
  }

  it should "deserialize a list into a mutable SortedSet" in { f =>
    val result = f.readValue[mutable.SortedSet[String]](setJson)
    result shouldBe setScala
  }

  it should "deserialize a list into an immutable TreeSet" in { f =>
    val result = f.readValue[immutable.TreeSet[String]](setJson)
    result shouldBe setScala
  }

  it should "deserialize a list into a mutable TreeSet" in { f =>
    val result = f.readValue[mutable.TreeSet[String]](setJson)
    result shouldBe setScala
  }

  it should "deserialize a list of Ints into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[Int]](intSetJson)
    result shouldBe intSetScala
  }

  it should "deserialize a list of Longs into a SortedSet" in { f =>
    val result = f.readValue[LongSetHolder](longSetHolderJson)
    result shouldBe longSetHolderScala
  }

  it should "deserialize a list of Comparables into a SortedSet" in { f =>
    val result = f.readValue[SortedSet[ComparableBean]](comparableSetJson)
    result shouldBe comparableSetScala
  }

  it should "deserialize a lit of Ints into a SortedSet of Options" in { f =>
    // NB: This is `java.lang.Integer`, because of GH-104
    val result = f.readValue[SortedSet[Option[Integer]]](intSetJson, new TypeReference[SortedSet[Option[Integer]]]{})
    result shouldBe optionIntSetScala
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
