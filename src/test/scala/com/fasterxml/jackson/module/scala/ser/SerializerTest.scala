package __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser

import __foursquare_shaded__.com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.JacksonTest

trait SerializerTest extends JacksonTest {
  def serialize(value: Any, mapper: ObjectMapper = newMapper): String = mapper.writeValueAsString(value)

  def jsonOf(s: String): JsonNode = newMapper.readTree(s)
}
