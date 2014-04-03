package com.fasterxml.jackson.module.scala.ser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration, Weekday}
import com.fasterxml.jackson.core.`type`.TypeReference

object EnumerationSerializerTest {

  class WeekdayType extends TypeReference[Weekday.type]

  case class AnnotationHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

  case class AnnotationOptionHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Option[Weekday.Weekday])
}

@RunWith(classOf[JUnitRunner])
class EnumerationSerializerTest extends SerializerTest with FlatSpec with ShouldMatchers {
  import EnumerationSerializerTest._

  lazy val module = DefaultScalaModule

  behavior of "EnumerationSerializer"

  it should "serialize an annotated Enumeration" in {
    val holder = AnnotationHolder(Weekday.Fri)
    serialize(holder) should be ("""{"weekday":"Fri"}""")
  }

  it should "serialize an Enumeration" in {
		val day = Weekday.Fri
		serialize(day) should be ("""{"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}""")
	}

  it should "serialize an annotated Option[Enumeration]" in {
    val holder = AnnotationOptionHolder(Some(Weekday.Fri))
    serialize(holder) should be ("""{"weekday":"Fri"}""")
  }

}