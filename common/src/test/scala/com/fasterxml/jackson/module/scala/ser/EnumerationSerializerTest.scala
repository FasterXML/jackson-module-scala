package com.fasterxml.jackson
package module.scala
package ser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import core.`type`.TypeReference

object EnumerationSerializerTest {

  class WeekdayType extends TypeReference[Weekday.type]

  case class AnnotationHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

  case class AnnotationOptionHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Option[Weekday.Weekday])

  object OptionType extends Enumeration {
    val STRING = Value("string")
    val NUMBER = Value("number")
    val BOOLEAN = Value("boolean")
  }

  class OptionTypeReference extends TypeReference[OptionType.type]
  case class OptionTypeHolder(@JsonScalaEnumeration(classOf[OptionTypeReference]) optionType: OptionType.Value)
}

@RunWith(classOf[JUnitRunner])
class EnumerationSerializerTest extends SerializerTest {

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

  it should "serialize an annotated Enumeration with custom values" in {
    serialize(OptionTypeHolder(OptionType.STRING)) should be ("""{"optionType":"string"}""")
  }

  it should "serialize an annotated Option[Enumeration]" in {
    val holder = AnnotationOptionHolder(Some(Weekday.Fri))
    serialize(holder) should be ("""{"weekday":"Fri"}""")
  }

}
