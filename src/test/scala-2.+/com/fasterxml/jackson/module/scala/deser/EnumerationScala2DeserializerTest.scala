package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.{DefaultScalaModule, Weekday}

// see EnumerationDeserializerTest for tests that also pass in Scala3
class EnumerationScala2DeserializerTest extends DeserializerTest {
  import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerTest._

  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  it should "locate the annotation on BeanProperty fields" in {
    val weekdayMapJson = """{"weekdayMap":{"Mon":"Boo","Fri":"Hooray!"}}"""
    val result = deserialize(weekdayMapJson, classOf[HolderImpl])
    result.weekdayMap should contain key Weekday.Mon
  }
}
