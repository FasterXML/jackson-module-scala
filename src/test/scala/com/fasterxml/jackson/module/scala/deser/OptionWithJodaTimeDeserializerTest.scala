package com.fasterxml.jackson.module.scala.deser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.datatype.joda.JodaModule

private case class OptionalInt(x: Option[Int])

@RunWith(classOf[JUnitRunner])
class OptionWithJodaTimeDeserializerTest extends DeserializerTest {

  def module = DefaultScalaModule

  "DefaultScalaModule" should "deserialize a case class with Option without JodaModule" in {
    deserialize[OptionalInt](stringValue) should be (OptionalInt(Some(123)))
  }

  it should "deserialize a case class with Option with JodaModule" in {
    newMapper.registerModule(new JodaModule)
    deserialize[OptionalInt](stringValue) should be (OptionalInt(Some(123)))
  }

  val stringValue = """{"x":123}"""
}
