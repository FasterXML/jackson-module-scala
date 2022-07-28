package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{DeserializationContext, ObjectMapper, ValueDeserializer}
import tools.jackson.module.scala.{DefaultScalaModule}
import tools.jackson.module.scala.BaseSpec

abstract class A

case class A1(prop1: String) extends A
case class A2(prop1: String, prop2: String) extends A

class ADeserializer extends ValueDeserializer[A] {
  def deserialize(jp: JsonParser, c: DeserializationContext): A1 = {
    jp.skipChildren()
    jp.nextToken()
    A1("qwer")
  }
}

case class B(prop1: String, @JsonDeserialize(`using` = classOf[ADeserializer]) prop2: A)

object Util {
  def mapper: ObjectMapper = {
    val builder = JsonMapper.builder().addModule(new DefaultScalaModule)
    builder.build()
  }

  val jsonString = """{"prop1":"asdf","prop2":{"prop1":"qwer"}}"""

  def fromJson(str:String): B = mapper.readValue(str,classOf[B])
}

class InteropTest extends BaseSpec
{
  "Scala module" should "support JsonDeserialize in Scala" in {
    val v = Util.mapper.readValue(Util.jsonString, classOf[B])
    v shouldBe B("asdf", A1("qwer"))
  }
}
