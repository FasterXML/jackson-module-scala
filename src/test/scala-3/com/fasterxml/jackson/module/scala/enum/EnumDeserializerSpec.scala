package com.fasterxml.jackson.module.scala.`enum`

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumDeserializerSpec extends AnyWordSpec with Matchers {
  val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "EnumModule" should {
    "deserialize ColorEnum" in {
      val red = s""""${ColorEnum.Red}""""
      mapper.readValue(red, classOf[ColorEnum]) shouldEqual ColorEnum.Red
    }
    "fail deserialization of invalid ColorEnum" in {
      val json = s""""xyz""""
      intercept[IllegalArgumentException] {
        mapper.readValue(json, classOf[ColorEnum])
      }
    }
    "deserialize Colors" in {
      val colors = Colors(Set(ColorEnum.Red, ColorEnum.Green))
      val json = mapper.writeValueAsString(colors)
      mapper.readValue(json, classOf[Colors]) shouldEqual colors
    }
    "deserialize ColorEnum with non-singleton EnumModule" in {
      val red = s""""${ColorEnum.Red}""""
      mapper.readValue(red, classOf[ColorEnum]) shouldEqual ColorEnum.Red
    }
    "deserialize JavaCompatibleColorEnum" in {
      mapper.writeValueAsString(JavaCompatibleColorEnum.Red) shouldEqual s""""${JavaCompatibleColorEnum.Red}""""
    }
    "deserialize Car with ColorEnum" in {
      val red = s"""{"make":"Perodua","color":"${ColorEnum.Green}"}"""
      mapper.readValue(red, classOf[Car]) shouldEqual Car("Perodua", ColorEnum.Green)
    }
    "deserialize CtxCar with Ctx.ColorEnum" in {
      val red = s"""{"make":"Perodua","color":"${Ctx.ColorEnum.Green}"}"""
      mapper.readValue(red, classOf[CtxCar]) shouldEqual CtxCar("Perodua", Ctx.ColorEnum.Green)
    }
    "deserialize Enum as Map Key" in {
      val json = s"""{"Green":"green","Red":"red"}"""
      val map = mapper.readValue(json, new TypeReference[Map[ColorEnum, String]] {})
      map should have size 2
      map(ColorEnum.Green) shouldEqual "green"
      map(ColorEnum.Red) shouldEqual "red"
    }

  }
}
