package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.module.scala.DefaultScalaModule

object PlainClassDeserializerTest {
  class VarTestConstructor(var test: Int)
  class AnnotatedVarTestConstructor(@JsonProperty("t") var test: Int)
}

class PlainClassDeserializerTest extends DeserializerTest {
  import PlainClassDeserializerTest._

  def module: DefaultScalaModule.type = DefaultScalaModule

  "An ObjectMapper with DefaultScalaModule" should "deserialize a plain scala class with a var" in {
    val inst = deserialize("""{"test":1234}""", classOf[VarTestConstructor])
    inst.test shouldEqual 1234
  }

  it should "deserialize a plain scala class with an annotated var" in {
    val inst = deserialize("""{"t":1234}""", classOf[AnnotatedVarTestConstructor])
    inst.test shouldEqual 1234
  }

}
