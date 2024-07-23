package tools.jackson.module.scala.deser

import tools.jackson.module.scala.{DefaultScalaModule, Weekday}

// see EnumerationDeserializerTest for tests that also pass in Scala3
class EnumerationScala2DeserializerTest extends DeserializerTest {
  import tools.jackson.module.scala.deser.EnumerationDeserializerTest._

  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  it should "locate the annotation on BeanProperty fields" in {
    val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
    val result = deserialize(weekdayMapJson, classOf[HolderImpl])
    result.weekdayMap should contain key Weekday.Mon
  }

  //TODO fix test (works in v2.18.0)
  it should "deserialize a set of weekdays" ignore {
    val container = new EnumSetContainer
    val json = newMapper.writeValueAsString(container)
    val result = deserialize(json, classOf[EnumSetContainer])
    result.days shouldEqual container.days
  }
}
