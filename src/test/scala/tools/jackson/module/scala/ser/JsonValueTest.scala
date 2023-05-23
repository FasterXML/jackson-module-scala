package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.module.scala.DefaultScalaModule

import scala.annotation.meta.getter

object JsonValueTest {
  case class ValueClass(@(JsonValue @getter) value: String)
}

class JsonValueTest extends SerializerTest {
  import JsonValueTest._

  override def module = DefaultScalaModule

  "DefaultScalaModule" should "support @JsonValue" in {
    serialize(ValueClass("Foo")) should equal (""""Foo"""")
  }
}
