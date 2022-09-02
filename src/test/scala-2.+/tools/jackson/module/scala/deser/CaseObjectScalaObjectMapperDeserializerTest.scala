package tools.jackson.module.scala.deser

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.deser.CaseObjectDeserializerTest.TestObject
import tools.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}

//see also CaseObjectDeserializerTest
class CaseObjectScalaObjectMapperDeserializerTest extends DeserializerTest {
  def module = DefaultScalaModule

  "An ObjectMapper with ScalaObjectMapper and ScalaObjectDeserializerModule" should "deserialize a case object and not create a new instance" in {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(ScalaObjectDeserializerModule)
      .build() :: ScalaObjectMapper
    val original = TestObject
    val json = mapper.writeValueAsString(original)
    val deserialized = mapper.readValue[TestObject.type](json)
    assert(deserialized == original)
  }
}
