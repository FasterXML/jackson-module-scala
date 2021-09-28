package com.fasterxml.jackson
package module.scala
package ser

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.OuterWeekday.InnerWeekday

import scala.beans.BeanProperty

object EnumerationSerializerTest {

  class WeekdayType extends TypeReference[Weekday.type]

  case class AnnotationHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

  case class AnnotationOptionHolder(@JsonScalaEnumeration(classOf[WeekdayType]) weekday: Option[Weekday.Weekday])

  object OptionType extends Enumeration {
    val STRING: OptionType.Value = Value("string")
    val NUMBER: OptionType.Value = Value("number")
    val BOOLEAN: OptionType.Value = Value("boolean")
  }

  class OptionTypeReference extends TypeReference[OptionType.type]
  case class OptionTypeHolder(@JsonScalaEnumeration(classOf[OptionTypeReference]) @BeanProperty optionType: OptionType.Value)

  case class OptionTypeKeyedMapHolder(
    @JsonScalaEnumeration(classOf[OptionTypeReference]) @BeanProperty map: Map[OptionType.Value, String]
  )

  object EnumTest extends Enumeration {
    type EnumTest = Value
    val A, B = Value
  }
  class EnumTestType extends TypeReference[EnumTest.type]

  import EnumTest._
  case class EnumTestCaseClass(@JsonScalaEnumeration(classOf[EnumTestType]) a: EnumTest, label: String)
  case class EnumTestCaseClassWithExtraConstructors(@JsonScalaEnumeration(classOf[EnumTestType]) a: EnumTest, label: String) {
    def this(b: String) = this(EnumTest.A, "None")
    def this(a: EnumTest) = this(a, "None")
  }
}

class EnumerationSerializerTest extends SerializerTest {

  import EnumerationSerializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  behavior of "EnumerationSerializer"

  it should "serialize an annotated Enumeration" in {
    val holder = AnnotationHolder(Weekday.Fri)
    serialize(holder) shouldEqual """{"weekday":"Fri"}"""
  }

  it should "roundtrip an annotated Enumeration" in {
    val mapper = newMapper
    val holder = AnnotationHolder(Weekday.Fri)
    val json = serialize(holder, mapper)
    json shouldEqual """{"weekday":"Fri"}"""
    mapper.readValue(json, classOf[AnnotationHolder]) shouldEqual holder
  }

  it should "serialize an Enumeration" in {
		val day = Weekday.Fri
		serialize(day) should be ("""{"enumClass":"com.fasterxml.jackson.module.scala.Weekday","value":"Fri"}""")
	}

  it should "serialize an inner Enumeration" in {
    val day = InnerWeekday.Fri
    serialize(day) should be ("""{"enumClass":"com.fasterxml.jackson.module.scala.OuterWeekday$InnerWeekday","value":"Fri"}""")
  }

  it should "serialize an annotated Enumeration with custom values" in {
    serialize(OptionTypeHolder(OptionType.STRING)) should be ("""{"optionType":"string"}""")
  }

  it should "serialize an annotated Option[Enumeration]" in {
    val holder = AnnotationOptionHolder(Some(Weekday.Fri))
    serialize(holder) should be ("""{"weekday":"Fri"}""")
  }

  it should "serialize a Map keyed with annotated Option[Enumeration]" in {
    val holder = OptionTypeKeyedMapHolder(Map(OptionType.STRING -> "foo"))
    serialize(holder) shouldBe """{"map":{"string":"foo"}}"""
  }

  it should "serialize an case class with enum (case class has extra constructors)" in {
    val case1 = EnumTestCaseClass(EnumTest.A, "None")
    serialize(case1) shouldBe """{"a":"A","label":"None"}"""
    val case2 = EnumTestCaseClassWithExtraConstructors(EnumTest.A, "None")
    serialize(case2) shouldBe """{"a":"A","label":"None"}"""
  }
}
