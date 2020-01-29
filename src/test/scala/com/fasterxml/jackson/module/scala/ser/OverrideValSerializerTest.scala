package com.fasterxml.jackson.module.scala.ser

import java.util.UUID

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
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
    //TODO https://github.com/FasterXML/jackson-module-scala/issues/218
    //the json should also include "id":"<idAsString>"
    serialize(Sub(id, "something")) shouldBe """{"type":"baseclass","something":"something"}"""
  }
}
