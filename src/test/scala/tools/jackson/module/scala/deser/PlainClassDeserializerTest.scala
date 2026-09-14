package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.module.scala.DefaultScalaModule

object PlainClassDeserializerTest {
  class VarTestConstructor(var test: Int)
  class AnnotatedVarTestConstructor(@JsonProperty("t") var test: Int)

  case class Item(n: Int)

  trait ItemsHolder {
    var items: Map[String, Item] = Map.empty
  }

  class ItemsHolderImpl extends ItemsHolder
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

  // the class implements the trait's var with a setter Scala 3 emits without a generic signature,
  // which would leave the values read as maps rather than Items
  it should "deserialize a generic var taken from a trait" in {
    val inst = deserialize("""{"items":{"a":{"n":1}}}""", classOf[ItemsHolderImpl])
    inst.items shouldEqual Map("a" -> Item(1))
  }

}
