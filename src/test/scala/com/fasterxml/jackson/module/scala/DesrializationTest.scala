package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import reflect.BeanProperty
import org.codehaus.jackson.`type`.TypeReference
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DesrializationTest extends FlatSpec with ShouldMatchers {

	"An ObjectMapper with the ScalaModule" should "deserialize into a ListBuffer" in {
			val expectedList = (1 to 6).toList
			val hrm = deserializeWithModule(listJson, listBufferType)
			hrm should be === expectedList
	}

	it should "deserialize into a java ArrayList" in {
		val expectedArrayList = new java.util.ArrayList[Int]()
		(1 to 6).foreach(i => {
			expectedArrayList.add(i)
		})

		val hrm = deserializeWithModule(listJson, arrayListType)
		hrm should be === expectedArrayList
	}

	val listJson =  "[1,2,3,4,5,6]"
	val listBufferType = new TypeReference[collection.mutable.ListBuffer[Int]]() {}
	val arrayListType = new TypeReference[java.util.ArrayList[Int]]() {}

	/*

	it should "serialize an Enumeration" in {
		object Weekday extends Enumeration {
			type Weekday = Value
			val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
		}

		val day = Weekday.Fri
		deserializeWithModule(day) should be === """{"enumClass":"com.fasterxml.jackson.module.scala.SerializationTest$$anonfun$3$Weekday$2","value":"Fri"}"""
	}

	it should "serialize a mutable Map" in {
		val map = collection.mutable.HashMap("key1" -> "value1", "key2" -> "value2")
		deserializeWithModule(map) should be === """{"key1":"value1","key2":"value2"}"""
	}

	it should "serialize a Map" in {
		val map = Map("key1" -> "value1", "key2" -> "value2")
		deserializeWithModule(map) should be === """{"key1":"value1","key2":"value2"}"""
	}

	it should "serialize a bean" in {
		val bean = new Bean()
		deserializeWithModule(bean) should be === """{"name":"Dave","age":23}"""
	}

	it should "serialize lists, maps, and beans" in {
		val bean = new ComplexBean
		deserializeWithModule(bean) should be === """{"map":{"key":"value"},"favoriteNumbers":[1,2,3],"bean":{"name":"Dave","age":23}}"""
	}
	*/

	def deserializeWithModule(value: String, valueType: TypeReference[_]) : Any = {
		val mapper = new ObjectMapper()
		mapper.registerModule(new ScalaModule())
		mapper.readValue(value, valueType)
	}

	class Bean {

		@BeanProperty
		var name: String = "Dave"

		@BeanProperty
		var age: Integer = 23
	}

	class ComplexBean {

		@BeanProperty
		var bean: Bean = new Bean

		@BeanProperty
		var map: Map[String, String] = Map("key" -> "value")

		@BeanProperty
		var favoriteNumbers: List[Int] = (1 to 3).toList
	}
}