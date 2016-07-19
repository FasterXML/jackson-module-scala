package com.fasterxml.jackson.module.scala.deser

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Dog
{
  def apply(n: String, thinking: Boolean) = {
    val dog = new Dog
    dog.name = n
    dog.brain = new dog.Brain
    dog.brain.isThinking = thinking
    dog
  }
}
class Dog
{
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

  def module = DefaultScalaModule

  "Deserializer" should "support nested inner classes as values" in {
    val input = Dog("Smurf", thinking = true)
    val json = newMapper.writeValueAsString(input)
    val output = deserialize[Dog](json)

    output should have ('name ("Smurf"))
    output.brain should be ('thinking)
    output.brain should have ('parentName ("Smurf"))

    output.name = "Foo"
    output.brain should have ('parentName ("Foo"))
  }

}
