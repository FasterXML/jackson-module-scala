package tools.jackson.module.scala.`enum`.adt

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
    "deserialize ColorSet" in {
      val colors = ColorSet(Set(Color.Red, Color.Green))
      val json = mapper.writeValueAsString(colors)
      mapper.readValue(json, classOf[ColorSet]) shouldEqual colors
    }
  }
}
