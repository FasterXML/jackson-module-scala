package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import reflect.BeanProperty

/**
 */

class SerializationTest extends FlatSpec with ShouldMatchers {

	"An ObjectMapper without the ScalaModule" should "fail to serialize a List" in {
		val list = (1 to 6).toList
		serializeWithoutModule(list) should include regex (""""empty":false,"traversableAgain":true""")
	}

	"An ObjectMapper with the ScalaModule" should "serialize a List" in {
		val list = (1 to 6).toList
		serializeWithModule(list) should be === "[1,2,3,4,5,6]"
	}

	it should "serialize a Map" in {
		val map = Map("key1" -> "value1", "key2" -> "value2")
		serializeWithModule(map) should be === """{"key1":"value1","key2":"value2"}"""
	}

	it should "serialize a bean" in {
		val bean = new Bean()
		serializeWithModule(bean) should be === """{"name":"Dave","age":23}"""
	}

	it should "serialize lists, maps, and beans" in {
		val bean = new ComplexBean
		serializeWithModule(bean) should be === """{"map":{"key":"value"},"favoriteNumbers":[1,2,3],"bean":{"name":"Dave","age":23}}"""
	}

	def serializeWithoutModule(value: AnyRef) = {
		write(value, false)
	}

	def serializeWithModule(value: AnyRef) = {
		write(value, true)
	}

	def write(value: AnyRef, withModule: Boolean) = {
		val mapper = new ObjectMapper()

		if (withModule) {
			mapper.registerModule(new ScalaModule())
		}

		val writer = new StringWriter()
		mapper.writeValue(writer, value)
		writer.toString
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