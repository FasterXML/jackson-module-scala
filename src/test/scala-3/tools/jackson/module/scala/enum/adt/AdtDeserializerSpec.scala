package tools.jackson.module.scala.`enum`.adt

import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AdtDeserializerSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "EnumModule" should {
    "deserialize Color ADT" in {
      val red = s""""${Color.Red}""""
      mapper.readValue(red, classOf[Color]) shouldEqual Color.Red
    }
    "fail deserialization of invalid Color ADT" in {
      val json = s""""xyz""""
      intercept[IllegalArgumentException] {
        mapper.readValue(json, classOf[Color])
      }
    }
    "deserialize Color.Mix" in {
      val json = mapper.writeValueAsString(Color.Mix(0x4488FF))
      mapper.readValue(json, classOf[Color]) shouldEqual Color.Mix(0x4488FF)
    }
    "refuse a second @type rather than reading past it" in {
      val json = """{"@type":"Mix","@type":"Red","mix":4491519}"""
      val thrown = the[DatabindException] thrownBy mapper.readValue(json, classOf[Color])
      thrown.getMessage should include("Duplicate @type")
    }
    "deserialize ColorSet" in {
      val colors = ColorSet(Set(Color.Red, Color.Green, Color.Mix(0x4488FF)))
      val json = mapper.writeValueAsString(colors)
      mapper.readValue(json, classOf[ColorSet]) shouldEqual colors
    }
  }
}
