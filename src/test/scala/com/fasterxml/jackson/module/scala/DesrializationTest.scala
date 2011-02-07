package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import reflect.BeanProperty
import org.codehaus.jackson.`type`.TypeReference
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import collection.mutable.HashMap

@RunWith(classOf[JUnitRunner])
class DesrializationTest extends FlatSpec with ShouldMatchers {

	"An ObjectMapper with the ScalaModule" should "deserialize into a ListBuffer" in {
			val expectedList = (1 to 6).toList
			val hrm = deserializeWithModule(listJson, listBufferType)
			hrm should be === expectedList
	}

	it should "deserialize a Map" in {
		val expectedMap = new HashMap[String, String]()
		expectedMap += ("key1" -> "value")
		expectedMap += ("key2" -> "3")
		val hrm = deserializeWithModule(mapJson, stringToStringMapType)
		hrm should be === expectedMap
	}

	it should "deserialize into a java ArrayList" in {
		val expectedArrayList = new java.util.ArrayList[Int]()
		(1 to 6).foreach(i => {
			expectedArrayList.add(i)
		})

		val hrm = deserializeWithModule(listJson, arrayListType)
		hrm should be === expectedArrayList
	}

	it should "deserialize a complex bean" in {
		val expected = new ComplexBean
		val json = """{"map":{"key":"value"},"favoriteNumbers":[1,2,3],"bean":{"name":"Dave","age":23}}"""
		val hrm = deserializeBeanWithModule(json, classOf[ComplexBean])
		hrm should be === expected
	}

	val listJson =  "[1,2,3,4,5,6]"
	val mapJson = """{"key1":"value","key2":"3"}"""
	val stringToObjectMapJson = """{"key1":"value","key2":"3"}"""
	val complexBeanType = new TypeReference[ComplexBean]() {}
	val stringToStringMapType = new TypeReference[collection.mutable.Map[String, String]]() {}
	val listBufferType = new TypeReference[collection.mutable.ListBuffer[Int]]() {}
	val arrayListType = new TypeReference[java.util.ArrayList[Int]]() {}

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
}