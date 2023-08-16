package tools.jackson.module.scala.deser

import tools.jackson.module.scala.DefaultScalaModule

class RecordTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "not affect Java Record deserialization" in {
    val json = "{\"value\": \"a@b\"}"
    val mapper = newMapper
    val testVal = mapper.readValue(json, classOf[MRecordWrapper])
    testVal.value shouldEqual new MRecord("a", "b")
  }
}
