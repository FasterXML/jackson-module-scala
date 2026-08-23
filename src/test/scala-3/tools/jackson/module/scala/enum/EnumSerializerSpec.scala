package tools.jackson.module.scala.`enum`

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
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
    // parameterized cases are written as JSON objects tagged with a type marker
    // see https://github.com/FasterXML/jackson-module-scala/issues/831
    "serialize ResultEnum.Ok" in {
      mapper.writeValueAsString(Result(ResultEnum.Ok("my-result"))) shouldEqual s"""{"result":{"@type":"Ok","value":"my-result"}}"""
    }
    "serialize ResultEnum.Error" in {
      mapper.writeValueAsString(Result(ResultEnum.Error(123))) shouldEqual s"""{"result":{"@type":"Error","code":123}}"""
    }
    // simple cases of the same enum keep their plain name
    "serialize ResultEnum.Pending" in {
      mapper.writeValueAsString(Result(ResultEnum.Pending)) shouldEqual s"""{"result":"Pending"}"""
    }
    // the tag is `@type` so that it cannot clash with a field of the case itself
    "serialize a case that has its own type field" in {
      mapper.writeValueAsString(TypeFieldHolder(TypeFieldEnum.Typed("custom"))) shouldEqual
        s"""{"value":{"@type":"Typed","type":"custom"}}"""
    }
    "serialize ShapeEnumAnnotated.Circle" in {
      mapper.writeValueAsString(ShapeEnumAnnotatedHolder(ShapeEnumAnnotated.Circle(1.5))) shouldEqual
        s"""{"shape":{"type":"Circle","radius":1.5}}"""
    }
  }
}
