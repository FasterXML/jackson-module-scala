package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.{ObjectMapper, DeserializationContext, JsonDeserializer}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

abstract class A

case class A1(prop1: String) extends A
case class A2(prop1: String, prop2: String) extends A

class ADeserializer extends JsonDeserializer[A] {
  def deserialize(jp: JsonParser, c: DeserializationContext) = {
    jp.skipChildren()
    jp.nextToken()
    A1("qwer")
  }
}

case class B(prop1: String, @JsonDeserialize(using = classOf[ADeserializer]) prop2: A)

object Util {
  def mapper = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
    m
  }

  val jsonString = """{"prop1":"asdf","prop2":{"prop1":"qwer"}}"""

  def fromJson(str:String) = mapper.readValue(str,classOf[B])
}

@RunWith(classOf[JUnitRunner])
class InteropTest extends FlatSpec with ShouldMatchers
{
  "Scala module" should "support JsonDeserialize in Scala" in {
    val v = Util.mapper.readValue(Util.jsonString, classOf[B])
    v should be === B("asdf", A1("qwer"))
  }
}

