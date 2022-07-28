package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import tools.jackson.databind.MapperFeature
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

object TransientFieldTest {
  @JsonPropertyOrder(Array("x"))
  class ClassyTransient {
    val x = 42
    @transient
    val value = 3
    def getValue: Int = value
  }

  class IgnoredTransient {
    @transient
    val value = 3
    val x = 42
  }
}

class TransientFieldTest extends SerializerTest {
  import TransientFieldTest._

  val module: JacksonModule = DefaultScalaModule

  "DefaultScalaModule" should "normally ignore @transient annotations" in {
    serialize(new ClassyTransient) shouldBe """{"x":42,"value":3}"""
  }

  it should "respect @transient annotation when feature enabled" in {
    serialize(new ClassyTransient, newBuilder.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER).build()) shouldBe """{"x":42}"""
  }

   it should "normally ignore @transient fields without getters" in {
     serialize(new IgnoredTransient) shouldBe """{"x":42}"""
   }
}
