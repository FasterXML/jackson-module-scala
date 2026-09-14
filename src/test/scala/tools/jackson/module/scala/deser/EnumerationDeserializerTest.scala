package tools.jackson.module.scala.deser

import tools.jackson.module.scala.OuterWeekday.InnerWeekday
import tools.jackson.module.scala.ser.EnumerationSerializerTest.{AnnotationHolder, AnnotationOptionHolder, WeekdayType}
import tools.jackson.module.scala.{DefaultScalaModule, JsonScalaEnumeration}
import tools.jackson.module.scala.OuterWeekday.InnerWeekday
import tools.jackson.module.scala.Weekday
import tools.jackson.module.scala.ser.EnumerationSerializerTest.{AnnotationHolder, AnnotationOptionHolder, WeekdayType}

import tools.jackson.databind.DatabindException
import tools.jackson.databind.`type`.TypeFactory
import tools.jackson.databind.json.JsonMapper

import scala.beans.BeanProperty
import scala.collection.mutable

class EnumContainer {
  var day: Weekday.Value = Weekday.Fri
}

class EnumSetContainer {
  var days: Set[Weekday.Value] = Set(Weekday.Fri, Weekday.Sat, Weekday.Sun)
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

  trait EnumMapHolderTrait {
    @JsonScalaEnumeration(classOf[WeekdayType])
    var weekdayMap: Map[Weekday.Value, String] = Map.empty
  }

  class EnumMapHolderTraitImpl extends EnumMapHolderTrait
}

class EnumerationDeserializerTest extends DeserializerTest {
  import EnumerationDeserializerTest._

  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with EnumDeserializerModule" should "deserialize a value into a scala Enumeration as a bean property" in {
    val expectedDay = Weekday.Fri
    val result = deserialize(fridayEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  it should "deserialize a set of weekdays" in {
    val container = new EnumSetContainer
    container.days = Set(Weekday.Mon, Weekday.Tue)
    val json = newMapper.writeValueAsString(container)
    val result = deserialize(json, classOf[EnumSetContainer])
    result.days shouldEqual Set(Weekday.Mon, Weekday.Tue)
  }

  it should "deserialize a case class with annotated set of weekdays" in {
    val container = EnumSetAnnotatedCaseClass(Set(Weekday.Sat, Weekday.Sun))
    val json = newMapper.writeValueAsString(container)
    val result = deserialize(json, classOf[EnumSetAnnotatedCaseClass])
    result shouldEqual container
  }

  it should "deserialize a value of an inner Enumeration class into a scala Enumeration as a bean property" in {
    val expectedDay = InnerWeekday.Fri
    val result = deserialize(fridayInnerEnumJson, classOf[EnumContainer])
    result.day should be (expectedDay)
  }

  it should "locate the annotation on BeanProperty fields" in {
    val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
    val result = deserialize(weekdayMapJson, classOf[HolderImpl])
    result.weekdayMap should contain key Weekday.Mon
  }

  // the class implements the trait's var with a setter Scala 3 emits without a generic signature (#535)
  it should "deserialize a map keyed by an annotated Enumeration through a var taken from a trait" in {
    val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
    val result = deserialize(weekdayMapJson, classOf[EnumMapHolderTraitImpl])
    result.weekdayMap shouldEqual Map(Weekday.Mon -> "Boo", Weekday.Fri -> "Hooray!")
  }

  it should "deserialize an annotated Enumeration value (JsonScalaEnumeration)" in {
    val result = deserialize(annotatedFridayJson, classOf[AnnotationHolder])
    result.weekday should be (Weekday.Fri)
  }

  it should "deserialize an annotated optional Enumeration value (JsonScalaEnumeration)" in {
    val result = deserialize(annotatedFridayJson, classOf[AnnotationOptionHolder])
    result.weekday shouldBe Some(Weekday.Fri)
  }

  // read at the root rather than as a bean property: a property deserializer wraps what it catches,
  // so the root is where a raw reflection failure escapes readValue
  it should "report an enumClass it cannot find as a databind problem" in {
    val json = """{"enumClass":"tools.jackson.module.scala.NoSuchWeekday","value":"Fri"}"""
    val thrown = the[DatabindException] thrownBy newMapper.readValue(json, classOf[Weekday.Value])
    thrown.getMessage should include("NoSuchWeekday")
  }

  it should "report a value the Enumeration does not have as a databind problem" in {
    val json = """{"enumClass":"tools.jackson.module.scala.Weekday","value":"Caturday"}"""
    val thrown = the[DatabindException] thrownBy newMapper.readValue(json, classOf[Weekday.Value])
    thrown.getMessage should include("Caturday")
  }

  it should "resolve the enumClass through the mapper's own classloader" in {
    val asked = mutable.Buffer[String]()
    val recording = new ClassLoader(getClass.getClassLoader) {
      override def loadClass(name: String, resolve: Boolean): Class[_] = {
        asked += name
        super.loadClass(name, resolve)
      }
    }
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .typeFactory(TypeFactory.createDefaultInstance().withClassLoader(recording))
      .build()
    mapper.readValue(fridayEnumJson, classOf[EnumContainer]).day should be (Weekday.Fri)
    asked should contain ("tools.jackson.module.scala.Weekday$")
  }

  val fridayEnumJson = """{"day": {"enumClass":"tools.jackson.module.scala.Weekday","value":"Fri"}}"""

  val fridayInnerEnumJson = """{"day": {"enumClass":"tools.jackson.module.scala.OuterWeekday$InnerWeekday","value":"Fri"}}"""

  val annotatedFridayJson = """{"weekday":"Fri"}"""
}
