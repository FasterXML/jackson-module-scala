package com.fasterxml.jackson.module.scala.`enum`

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumSerializerSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "EnumModule" should {
    "not serialize None" in {
      mapper.writeValueAsString(None) should not equal s""""$None""""
    }
    "serialize ColorEnum" in {
      mapper.writeValueAsString(ColorEnum.Red) shouldEqual s""""${ColorEnum.Red}""""
    }
    "serialize Colors" in {
      val json = mapper.writeValueAsString(Colors(Set(ColorEnum.Red, ColorEnum.Green)))
      json should startWith("""{"set":[""")
      json should include(""""Red"""")
      json should include(""""Green"""")
    }
    "serialize ColorEnum with non-singleton EnumModule" in {
      mapper.writeValueAsString(ColorEnum.Red) shouldEqual s""""${ColorEnum.Red}""""
    }
    "serialize JavaCompatibleColorEnum" in {
      mapper.writeValueAsString(ColorEnum.Red) shouldEqual s""""${ColorEnum.Red}""""
    }
    "serialize Car with ColorEnum" in {
      mapper.writeValueAsString(Car("Perodua", ColorEnum.Green)) shouldEqual s"""{"make":"Perodua","color":"${ColorEnum.Green}"}"""
    }
    "serialize CtxCar with Ctx.ColorEnum" in {
      mapper.writeValueAsString(CtxCar("Perodua", Ctx.ColorEnum.Green)) shouldEqual s"""{"make":"Perodua","color":"${Ctx.ColorEnum.Green}"}"""
    }
    "serialize Enum as Map Key" in {
      mapper.writeValueAsString(Map(ColorEnum.Green -> "green")) shouldEqual s"""{"Green":"green"}"""
    }
  }
}
