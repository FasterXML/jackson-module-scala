package com.fasterxml.jackson.module.scala.ser

import java.util.UUID

import com.fasterxml.jackson.module.scala.DefaultScalaModule

class OverrideValSerializerTest extends SerializerTest {
  lazy val module = DefaultScalaModule

  trait MyTrait {
    val id: UUID
    val `type`: String
  }

  class Base(val id: UUID) extends MyTrait {
    override val `type`: String = "baseclass"
  }

  case class Sub(override val id: UUID, something: String) extends Base(id)

  "DefaultScalaModule" should "handle overrides in vals" in {
    val id = UUID.randomUUID()
    //https://github.com/FasterXML/jackson-module-scala/issues/218
    val json = serialize(Sub(id, "something"))
    json should include (s""""id":"${id.toString}"""")
    json should include (""""type":"baseclass"""")
    json should include (""""something":"something"""")
  }
}
