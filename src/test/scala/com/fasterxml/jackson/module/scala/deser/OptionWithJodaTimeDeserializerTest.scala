package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

private case class OptionalInt(x: Option[Int])

@RunWith(classOf[JUnitRunner])
class OptionWithJodaTimeDeserializerTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "DefaultScalaModule" should "deserialize a case class with Option without JodaModule" in {
    deserializeWithManifest[OptionalInt](stringValue) should be (OptionalInt(Some(123)))
  }

  it should "deserialize a case class with Option with JodaModule" in {
    val builder = JsonMapper.builder().addModules(new DefaultScalaModule, new JodaModule)
    val mapper = builder.build()
    mapper.readValue(stringValue, typeReference[OptionalInt]) should be (OptionalInt(Some(123)))
  }

  val stringValue = """{"x":123}"""
}
