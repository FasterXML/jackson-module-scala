package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectReader}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.{Map, mutable}

case class ClassWithLists(field1: List[String], @JsonMerge field2: List[String])
case class ClassWithMaps[T](field1: Map[String, T], @JsonMerge field2: Map[String, T])
case class ClassWithMutableMaps[T](field1: mutable.Map[String, T], @JsonMerge field2: mutable.Map[String, T])

case class Pair(first: String, second: String)

class MergeTest extends DeserializerTest {

  val module: DefaultScalaModule.type = DefaultScalaModule

  def newScalaMapper: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(module)
    mapper
  }

  def newMergeableScalaMapper: ObjectMapper = {
    val mapper = newScalaMapper
    mapper.setDefaultMergeable(true)
    mapper
  }

  behavior of "The DefaultScalaModule when reading for updating"

  it should "merge both lists" in {
    val clazz = classOf[ClassWithLists]
    val initial = deserialize(classJson(firstListJson), clazz)
    val result = updateValue(newMergeableScalaMapper, initial, clazz, classJson(secondListJson))

    result shouldBe ClassWithLists(mergedList, mergedList)
  }

  it should "merge only the annotated list" in {
    val clazz = classOf[ClassWithLists]
    val initial = deserialize(classJson(firstListJson), clazz)
    val result = updateValue(newScalaMapper, initial, clazz, classJson(secondListJson))

    result shouldBe ClassWithLists(secondList, mergedList)
  }

  it should "merge both string maps" in {
    val typeReference = new TypeReference[ClassWithMaps[String]] {}
    val initial = deserialize(classJson(firstStringMapJson), typeReference)
    val result = updateValue(newMergeableScalaMapper, initial, typeReference, classJson(secondStringMapJson))

    result shouldBe ClassWithMaps(mergedStringMap, mergedStringMap)
  }

  it should "merge only the annotated string map" in {
    val typeReference = new TypeReference[ClassWithMaps[String]] {}
    val initial = deserialize(classJson(firstStringMapJson), typeReference)
    val result = updateValue(newScalaMapper, initial, typeReference, classJson(secondStringMapJson))

    result shouldBe ClassWithMaps(secondStringMap, mergedStringMap)
  }

  it should "merge both pair maps" in {
    val typeReference = new TypeReference[ClassWithMaps[Pair]] {}
    val initial = deserialize(classJson(firstPairMapJson), typeReference)
    val result = updateValue(newMergeableScalaMapper, initial, typeReference, classJson(secondPairMapJson))

    result shouldBe ClassWithMaps(mergedPairMap, mergedPairMap)
  }

  it should "merge only the annotated pair map" in {
    val typeReference = new TypeReference[ClassWithMaps[Pair]]{}
    val initial = deserialize(classJson(firstPairMapJson), typeReference)
    val result = updateValue(newScalaMapper, initial, typeReference, classJson(secondPairMapJson))

    result shouldBe ClassWithMaps(secondPairMap, mergedPairMap)
  }

  it should "merge both mutable maps" in {
    val typeReference = new TypeReference[ClassWithMutableMaps[String]]{}
    val initial = deserialize(classJson(firstStringMapJson), typeReference)
    val result = updateValue(newMergeableScalaMapper, initial, typeReference, classJson(secondStringMapJson))

    result shouldBe ClassWithMutableMaps(mutable.Map() ++ mergedStringMap, mutable.Map() ++ mergedStringMap)
  }

  it should "merge only the annotated mutable map" in {
    val typeReference = new TypeReference[ClassWithMutableMaps[String]]{}
    val initial = deserialize(classJson(firstStringMapJson), typeReference)
    val result = updateValue(newScalaMapper, initial, typeReference, classJson(secondStringMapJson))

    result shouldBe ClassWithMutableMaps(mutable.Map() ++ secondStringMap, mutable.Map() ++ mergedStringMap)
  }

  def classJson(nestedJson: String) = s"""{"field1":$nestedJson,"field2":$nestedJson}"""

  val firstListJson = """["one","two"]"""
  val secondListJson = """["three"]"""
  val secondList = List("three")
  val mergedList = List("one", "two", "three")

  val firstStringMapJson = """{"one":"1","two":"2"}"""
  val secondStringMapJson = """{"two":"22","three":"33"}"""
  val secondStringMap = Map("two" -> "22", "three" -> "33")
  val mergedStringMap = Map("one" -> "1", "two" -> "22", "three" -> "33")

  val firstPairMapJson = """{"one":{"first":"1"},"two":{"second":"2"},"three":{"first":"3","second":"4"}}"""
  val secondPairMapJson = """{"two":{"first":"22"},"three":{"second":"33"}}"""
  val secondPairMap = Map("two" -> Pair("22", null), "three" -> Pair(null, "33"))
  val mergedPairMap = Map("one" -> Pair("1", null), "two" -> Pair("22", "2"), "three" -> Pair("3", "33"))

  private def updateValue[T](mapper: ObjectMapper, valueToUpdate: T,
                             typeReference: TypeReference[T], src: String): T = {
    objectReaderFor(mapper, valueToUpdate, typeReference).readValue(src)
  }

  private def updateValue[T](mapper: ObjectMapper, valueToUpdate: T,
                             clazz: Class[T], src: String): T = {
    objectReaderFor(mapper, valueToUpdate, clazz).readValue(src)
  }

  private def objectReaderFor[T](mapper: ObjectMapper, valueToUpdate: T,
                                 typeReference: TypeReference[T]): ObjectReader = {
    mapper.readerForUpdating(valueToUpdate).forType(typeReference)
  }

  private def objectReaderFor[T](mapper: ObjectMapper, valueToUpdate: T,
                                 clazz: Class[T]): ObjectReader = {
    mapper.readerForUpdating(valueToUpdate).forType(clazz)
  }
}
