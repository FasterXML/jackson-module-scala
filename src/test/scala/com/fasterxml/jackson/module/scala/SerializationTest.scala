package com.fasterxml.jackson.module.scala

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.codehaus.jackson.map.ObjectMapper
import java.io.StringWriter
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SerializationTest extends FlatSpec with ShouldMatchers {

	"An ObjectMapper without the ScalaModule" should "fail to serialize a List" in {
		val list = (1 to 6).toList
		serializeWithoutModule(list) should include regex (""""empty":false,"traversableAgain":true""")
	}

	"An ObjectMapper with the ScalaModule" should "serialize a List" in {
		val list = (1 to 6).toList
		serializeWithModule(list) should be === "[1,2,3,4,5,6]"
	}

	it should "serialize an Enumeration" in {
		val day = Weekday.Fri
		serializeWithModule(day) should be === """{"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}"""
	}

	it should "serialize a mutable Map" in {
		val map = collection.mutable.HashMap("key1" -> "value1", "key2" -> "value2")
		// one cannot rely on map iteration order
		serializeWithModule(map) should (be === """{"key1":"value1","key2":"value2"}""" or be === """{"key2":"value2","key1":"value1"}""")
	}

	it should "serialize a Map" in {
		val map = Map("key1" -> "value1", "key2" -> "value2")
		// one cannot rely on map iteration order
		serializeWithModule(map) should (be === """{"key1":"value1","key2":"value2"}""" or be === """{"key2":"value2","key1":"value1"}""")
	}

  it should "serialize a SortedMap" in {
    val map = collection.immutable.TreeMap("key1" -> "value1", "key2" -> "value2")
    serializeWithModule(map) should (be === """{"key1":"value1","key2":"value2"}""")
  }

	it should "serialize a bean" in {
		val bean = new Bean()
		// one cannot rely on bean iteration order
		serializeWithModule(bean) should (be === """{"name":"Dave","age":23}""" or be === """{"age":23,"name":"Dave"}""")
	}

	it should "serialize lists, maps, and beans" in {
		val bean = new ComplexBean
    // one cannot rely on bean iteration order, so we have to be creative
    val ser = serializeWithModule(bean)
    // This might be overkill, but I couldn't come up with anything simpler for now
    val keyPattern = """"([a-zA-Z]+)""""
    val objectValuePattern = """\{[^\}]*\}"""
    val listValuePattern = """\[[^\]]*\]"""
    val valuePattern = String.format("(%s|%s)", objectValuePattern, listValuePattern)
    val keyValuePattern = String.format("%s:%s", keyPattern, valuePattern)
    val FullPattern = String.format("""\{%1$s,%1$s,%1$s\}""", keyValuePattern).r

    ser should fullyMatch regex(FullPattern)
    ser match {
      case FullPattern(key1,value1,key2,value2,key3,value3) => {
        val patternMap = Map(key1 -> value1, key2 -> value2, key3 -> value3)
        patternMap should contain key ("bean")
        patternMap("bean") should (be === """{"name":"Dave","age":23}""" or be === """{"age":23,"name":"Dave"}""")
        patternMap should contain key ("map")
        patternMap("map") should be === """{"key":"value"}"""
        patternMap should contain key("favoriteNumbers")
        patternMap("favoriteNumbers") should be === "[1,2,3]"
      }
    }
	}

	it should "serializer the keys from a map" in {
		val map = collection.mutable.HashMap("key1" -> "value1", "key2" -> "value2")
		// one cannot rely on map iteration order
		serializeWithModule(map.keys) should (be === """["key1","key2"]""" or be === """["key2","key1"]""")
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
}