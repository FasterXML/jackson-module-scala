package tools.jackson.module.scala.`enum`

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.exc.InvalidDefinitionException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnumDeserializerSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

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
    // see https://github.com/FasterXML/jackson-module-scala/issues/831
    "deserialize ResultEnum.Ok" ignore {
      val instance = Result(ResultEnum.Ok("my-result"))
      val json = mapper.writeValueAsString(instance)
      mapper.readValue(json, classOf[Result]) shouldEqual instance
    }
    // see https://github.com/FasterXML/jackson-module-scala/issues/831
    "deserialize ResultEnum.Error" ignore {
      val instance = Result(ResultEnum.Error(123))
      val json = mapper.writeValueAsString(instance)
      mapper.readValue(json, classOf[Result]) shouldEqual instance
    }
    // see https://github.com/FasterXML/jackson-module-scala/issues/831
    "deserialize ResultEnum.Pending" ignore {
      val instance = Result(ResultEnum.Pending)
      val json = mapper.writeValueAsString(instance)
      mapper.readValue(json, classOf[Result]) shouldEqual instance
    }
    "deserialize ShapeEnumAnnotated.Circle" in {
      val instance = ShapeEnumAnnotatedHolder(ShapeEnumAnnotated.Circle(1.5))
      val json = mapper.writeValueAsString(instance)
      mapper.readValue(json, classOf[ShapeEnumAnnotatedHolder]) shouldEqual instance
    }
  }
}
