package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.{DefaultScalaModule, JacksonModule}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

object Dog {
  def apply(n: String, thinking: Boolean): Dog = {
    val dog = new Dog
    dog.name = n
    dog.brain = new dog.Brain
    dog.brain.isThinking = thinking
    dog
  }
}

class Dog {
  var name: String = _
  var brain: Brain = _

  class Brain {
    var isThinking: Boolean = _
    def parentName = name
  }

}

// Cribbed from the same named test in jackson-databind
@RunWith(classOf[JUnitRunner])
class TestInnerClass extends DeserializerTest {

  def module: JacksonModule = DefaultScalaModule

  "Deserializer" should "support nested inner classes as values" in {
    val input = Dog("Smurf", thinking = true)
    val json = newMapper.writeValueAsString(input)
    val output = deserialize[Dog](json)

    output should have (Symbol("name") ("Smurf"))
    output.brain should be (Symbol("thinking"))
    output.brain should have (Symbol("parentName") ("Smurf"))

    output.name = "Foo"
    output.brain should have (Symbol("parentName") ("Foo"))
  }
}
