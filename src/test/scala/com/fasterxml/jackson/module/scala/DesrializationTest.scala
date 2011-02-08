package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.`type`.TypeReference
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import collection.mutable.HashMap
import org.codehaus.jackson.map.ObjectMapper

@RunWith(classOf[JUnitRunner])
class DesrializationTest extends FlatSpec with ShouldMatchers {

	"An ObjectMapper with the ScalaModule" should "deserialize a list into a ListBuffer" in {
		val expectedList = (1 to 6).toList
		deserializeWithModule(listJson, listBufferType) should be === expectedList
	}

	it should "deserialize a value into a scala Enumeration as a bean property" in {
		val expectedDay = Weekday.Fri
		deserializeBeanWithModule(fridayEnumJson, classOf[EnumContainer]).asInstanceOf[EnumContainer].day should be === expectedDay
	}

	it should "deserialize a json map into a mutable HashMap" in {
		val expectedMap = new HashMap[String, String]()
		expectedMap += ("key1" -> "value")
		expectedMap += ("key2" -> "3")
		deserializeWithModule(mapJson, stringToStringMapType) should be === expectedMap
	}

	it should "deserialize a json list into a java ArrayList" in {
		val expectedArrayList = new java.util.ArrayList[Int]()
		(1 to 6).foreach(i => {
			expectedArrayList.add(i)
		})

		deserializeWithModule(listJson, arrayListType) should be === expectedArrayList
	}

	it should "deserialize complex json into a complex scala bean" in {
		val expected = new ComplexBean
		val json = """{"map":{"key":"value"},"favoriteNumbers":[1,2,3],"bean":{"name":"Dave","age":23}}"""
		deserializeBeanWithModule(json, classOf[ComplexBean]) should be === expected
	}

	def deserializeBeanWithModule(json: String, clazz: Class[_]) : Any = {
		val mapper = new ObjectMapper()
		mapper.registerModule(new ScalaModule())
		mapper.readValue(json, clazz)
	}

	def deserializeWithModule(value: String, valueType: TypeReference[_]) : Any = {
		val mapper = new ObjectMapper()
		mapper.registerModule(new ScalaModule())
		mapper.readValue(value, valueType)
	}

	val listJson =  "[1,2,3,4,5,6]"
	val mapJson = """{"key1":"value","key2":"3"}"""
	val stringToObjectMapJson = """{"key1":"value","key2":"3"}"""
	val fridayEnumJson = """{"day": {"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}}"""
	val complexBeanType = new TypeReference[ComplexBean]() {}
	val stringToStringMapType = new TypeReference[collection.mutable.Map[String, String]]() {}
	val listBufferType = new TypeReference[collection.mutable.ListBuffer[Int]]() {}
	val arrayListType = new TypeReference[java.util.ArrayList[Int]]() {}
}