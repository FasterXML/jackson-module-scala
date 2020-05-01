package __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser

import __foursquare_shaded__.com.fasterxml.jackson.annotation.JsonValue
import __foursquare_shaded__.com.fasterxml.jackson.databind.Module
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.annotation.meta.getter

object TestJsonValue {
  case class ValueClass(@(JsonValue @getter) value: String)
}

@RunWith(classOf[JUnitRunner])
class TestJsonValue extends SerializerTest {
  import TestJsonValue._

  override def module: Module = DefaultScalaModule

  "DefaultScalaModule" should "support @JsonValue" in {
    serialize(ValueClass("Foo")) should equal (""""Foo"""")
  }
}
