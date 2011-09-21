package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.reflect.BeanProperty
import com.fasterxml.jackson.module.scala.Weekday

class EnumContainer {

	@BeanProperty
	var day = Weekday.Fri
}

@RunWith(classOf[JUnitRunner])
class EnumerationDeserializerTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new EnumerationDeserializerModule {}

	"An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
		val expectedDay = Weekday.Fri
    val result = deserialize(fridayEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
	}

	val fridayEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}}"""
}