package tools.jackson.module.scala.ser

import tools.jackson.databind.{JsonNode, ObjectMapper}
import tools.jackson.module.scala.JacksonTest

trait SerializerTest extends JacksonTest {
  def serialize(value: Any, mapper: ObjectMapper = newMapper): String = mapper.writeValueAsString(value)

  def jsonOf(s: String): JsonNode = newMapper.readTree(s)
}
