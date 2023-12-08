package tools.jackson.module.scala.`enum`.adt

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AdtSerializerSpec extends AnyWordSpec with Matchers {
  private val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

  "EnumModule" should {
    "serialize Color ADT" in {
      mapper.writeValueAsString(Color.Red) shouldEqual (s""""${Color.Red}"""")
    }
    "serialize Color.Mix" in {
      mapper.writeValueAsString(Color.Mix(0x4488FF)) shouldEqual (s""""Mix(4491519)"""")
    }
    "serialize ColorSet" in {
      val json = mapper.writeValueAsString(ColorSet(Set(Color.Red, Color.Green)))
      json should startWith("""{"set":[""")
      json should include(""""Red"""")
      json should include(""""Green"""")
    }
  }
}
