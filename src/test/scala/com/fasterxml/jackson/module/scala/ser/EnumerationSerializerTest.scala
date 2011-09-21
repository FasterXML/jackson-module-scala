package com.fasterxml.jackson.module.scala.ser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.fasterxml.jackson.module.scala.Weekday

@RunWith(classOf[JUnitRunner])
class EnumerationSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {

  lazy val module = new EnumerationSerializerModule {}

	it should "serialize an Enumeration" in {
		val day = Weekday.Fri
		serialize(day) should be ("""{"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}""")
	}

}