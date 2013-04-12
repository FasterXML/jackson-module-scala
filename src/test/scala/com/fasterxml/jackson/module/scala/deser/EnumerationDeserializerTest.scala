package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.reflect.BeanProperty
import com.fasterxml.jackson.module.scala.{JsonScalaEnumeration, DefaultScalaModule, Weekday}
import com.fasterxml.jackson.core.`type`.TypeReference

class EnumContainer {

	var day = Weekday.Fri
}

class WeekdayType extends TypeReference[Weekday.type]
case class AnnotatedEnumHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

@RunWith(classOf[JUnitRunner])
class EnumerationDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = DefaultScalaModule

	"An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
		val expectedDay = Weekday.Fri
    val result = deserialize[EnumContainer](fridayEnumJson)
    result.day should be (expectedDay)
	}

  it should "deserialize an annotated Enumeration value" in {
    val result = deserialize[AnnotatedEnumHolder](annotatedFridayJson)
    result.weekday should be (Weekday.Fri)
  }

	val fridayEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}}"""

  val annotatedFridayJson = """{"weekday":"Fri"}"""
}