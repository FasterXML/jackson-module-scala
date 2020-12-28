package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.OuterWeekday.InnerWeekday
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration, Weekday}

import scala.beans.BeanProperty

class EnumContainer {
  var day: Weekday.Value = Weekday.Fri
}

class WeekdayType extends TypeReference[Weekday.type]
case class AnnotatedEnumHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

class EnumMapHolder {
  @JsonScalaEnumeration(classOf[WeekdayType])
  var weekdayMap: Map[Weekday.Value, String] = Map.empty
}

object EnumerationDeserializerTest  {
  trait BeanPropertyEnumMapHolder {
    @BeanProperty
    @JsonScalaEnumeration(classOf[WeekdayType])
    var weekdayMap: Map[Weekday.Value, String] = Map.empty
  }

  class HolderImpl extends BeanPropertyEnumMapHolder
}

class EnumerationDeserializerTest extends DeserializerTest {
  import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerTest._

  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
    val expectedDay = Weekday.Fri
    val result = deserialize(fridayEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  "An ObjectMapper with EnumDeserializerModule" should "deserialize a value of an inner Enumeration class into a scala Enumeration as a bean property" in {
    val expectedDay = InnerWeekday.Fri
    val result = deserialize(fridayInnerEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  it should "deserialize an annotated Enumeration value" in {
    val result = deserialize(annotatedFridayJson, classOf[AnnotatedEnumHolder])
    result.weekday should be (Weekday.Fri)
  }

  it should "deserialize an annotated Enumeration as a key" in {
    val result = deserialize(weekdayMapJson, classOf[EnumMapHolder])
    result.weekdayMap should contain key Weekday.Mon
  }

  it should "locate the annotation on BeanProperty fields" in {
    val result = deserialize(weekdayMapJson, classOf[HolderImpl])
    result.weekdayMap should contain key Weekday.Mon
  }

  val fridayEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}}"""

  val fridayInnerEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.OuterWeekday$InnerWeekday","value":"Fri"}}"""

  val annotatedFridayJson = """{"weekday":"Fri"}"""

  val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
}
