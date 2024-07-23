package tools.jackson.module.scala.deser

import tools.jackson.module.scala.OuterWeekday.InnerWeekday
import tools.jackson.module.scala.ser.EnumerationSerializerTest.{AnnotationHolder, AnnotationOptionHolder, WeekdayType}
import tools.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration}
import tools.jackson.module.scala.OuterWeekday.InnerWeekday
import tools.jackson.module.scala.Weekday
import tools.jackson.module.scala.ser.EnumerationSerializerTest.{AnnotationHolder, AnnotationOptionHolder, WeekdayType}

import scala.beans.BeanProperty

class EnumContainer {
  var day: Weekday.Value = Weekday.Fri
}

class EnumSetContainer {
  val days: Set[Weekday.Value] = Set(Weekday.Fri, Weekday.Sat, Weekday.Sun)
}

case class EnumSetAnnotatedCaseClass(@JsonScalaEnumeration(classOf[WeekdayType]) days: Set[Weekday.Value])

class EnumMapHolder {
  @JsonScalaEnumeration(classOf[WeekdayType])
  var weekdayMap: Map[Weekday.Value, String] = Map.empty
}

object EnumerationDeserializerTest  {
  trait BeanPropertyEnumMapHolder {
    @BeanProperty
    @JsonScalaEnumeration(classOf[WeekdayType])
    var weekdayMap: Map[Weekday.Value, String] = Map.empty
  }

  class HolderImpl extends BeanPropertyEnumMapHolder
}

// see EnumerationScala2DeserializerTest for tests that only work in Scala2
class EnumerationDeserializerTest extends DeserializerTest {
  import EnumerationDeserializerTest._
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
    val expectedDay = Weekday.Fri
    val result = deserialize(fridayEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  //ignored because JsonScalaEnumeration causes issues when used on sets (and probably other collections)
  it should "deserialize a case class with annotated set of weekdays" ignore {
    val container = EnumSetAnnotatedCaseClass(Set(Weekday.Sat, Weekday.Sun))
    val json = newMapper.writeValueAsString(container)
    val result = deserialize(json, classOf[EnumSetContainer])
    result.days shouldEqual container.days
  }

  it should "deserialize a value of an inner Enumeration class into a scala Enumeration as a bean property" in {
    val expectedDay = InnerWeekday.Fri
    val result = deserialize(fridayInnerEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  it should "deserialize an annotated Enumeration value (JsonScalaEnumeration)" in {
    val result = deserialize(annotatedFridayJson, classOf[AnnotationHolder])
    result.weekday should be (Weekday.Fri)
  }

  it should "deserialize an annotated optional Enumeration value (JsonScalaEnumeration)" in {
    val result = deserialize(annotatedFridayJson, classOf[AnnotationOptionHolder])
    result.weekday shouldBe Some(Weekday.Fri)
  }

  it should "locate the annotation on BeanProperty fields" in {
    val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
    val result = deserialize(weekdayMapJson, classOf[HolderImpl])
    result.weekdayMap should contain key Weekday.Mon
  }

  val fridayEnumJson = """{"day": {"enumClass":"tools.jackson.module.scala.Weekday","value":"Fri"}}"""

  val fridayInnerEnumJson = """{"day": {"enumClass":"tools.jackson.module.scala.OuterWeekday$InnerWeekday","value":"Fri"}}"""

  val annotatedFridayJson = """{"weekday":"Fri"}"""
}
