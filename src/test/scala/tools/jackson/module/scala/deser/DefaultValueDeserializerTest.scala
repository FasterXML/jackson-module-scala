package tools.jackson.module.scala.deser

import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.scala.DefaultScalaModule

object DefaultValueDeserializerTest {

  case class Defaulted(id: Int, name: String = "") {
    def this() = this(1)
  }

}

class DefaultValueDeserializerTest extends DeserializerTest {
  import DefaultValueDeserializerTest._
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "deserialize defaulted parameters correctly" in {
    val json = newMapper.writeValueAsString(Defaulted(id = 1))
    json shouldBe """{"id":1,"name":""}"""
    val d = deserialize(json, classOf[Defaulted])
    d.name shouldEqual ""
  }

  it should "deserialize defaulted parameters correctly (ignores 2nd constructor)" in {
    // this may not be ideal but it is the existing behaviour so we will probably need
    // a config or annotation to get the test to use the 2nd constructor
    val json = """{"name":"123"}"""
    val mapper = newBuilder
      .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
      .build()

    val d = mapper.readValue(json, classOf[Defaulted])
    d.id shouldEqual 0
    d.name shouldEqual "123"
  }
}
