package com.fasterxml.jackson.module.scala.deser

import java.util.UUID

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidFormatException

import scala.collection._

object SortedSetDeserializerTest {
  case class LongSetHolder(@JsonDeserialize(contentAs = classOf[java.lang.Long]) value: SortedSet[Long])

  case class ComparableBean(value: Long) extends Comparable[ComparableBean] {
    def compareTo(o: ComparableBean): Int = value compareTo o.value
  }
}

class SortedSetDeserializerTest extends DeserializationFixture {
  import SortedSetDeserializerTest._

  behavior of "SortedSetDeserializer"

  it should "deserialize a list into a SortedSet" in { f =>
    val result = f.readValue(setJson, new TypeReference[SortedSet[String]]{})
    result shouldBe setScala
  }

  it should "deserialize a list into an immutable SortedSet" in { f =>
    val result = f.readValue(setJson, new TypeReference[immutable.SortedSet[String]]{})
    result shouldBe setScala
  }

  it should "deserialize a list into a mutable SortedSet" in { f =>
    val result = f.readValue(setJson, new TypeReference[mutable.SortedSet[String]]{})
    result shouldBe setScala
  }

  it should "deserialize a list into an immutable TreeSet" in { f =>
    val result = f.readValue(setJson, new TypeReference[immutable.TreeSet[String]]{})
    result shouldBe setScala
  }

  it should "deserialize a list into a mutable TreeSet" in { f =>
    val result = f.readValue(setJson, new TypeReference[mutable.TreeSet[String]]{})
    result shouldBe setScala
  }

  it should "deserialize a list of Ints into a SortedSet" in { f =>
    val result = f.readValue(intSetJson, new TypeReference[SortedSet[Integer]]{})
    result shouldBe intSetScala
  }

  it should "deserialize a list of Longs into a SortedSet" in { f =>
    val result = f.readValue(longSetHolderJson, new TypeReference[LongSetHolder]{})
    result shouldBe longSetHolderScala
  }

  it should "deserialize a list of Comparables into a SortedSet" in { f =>
    val result = f.readValue(comparableSetJson, new TypeReference[SortedSet[ComparableBean]]{})
    result shouldBe comparableSetScala
  }

  it should "deserialize a lit of Ints into a SortedSet of Options" in { f =>
    // NB: This is `java.lang.Integer`, because of GH-104
    val result = f.readValue(intSetJson, new TypeReference[SortedSet[Option[Integer]]]{})
    result shouldBe optionIntSetScala
  }

  it should "keep path index if error happened" in { f =>
    import scala.collection.JavaConverters._
    val uuidListJson = """["13dfbd92-dbc5-41cc-8d93-22079acc09c4", "foo"]"""
    val exception = intercept[InvalidFormatException] {
      f.readValue(uuidListJson, new TypeReference[SortedSet[Option[UUID]]]{})
    }

    val exceptionPath = exception.getPath.asScala.map(_.getIndex)
    exceptionPath should equal (List(1))
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
