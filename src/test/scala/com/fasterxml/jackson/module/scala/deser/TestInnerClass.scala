package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
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
class TestInnerClass extends DeserializerTest with FlatSpec with ShouldMatchers {

  def module = DefaultScalaModule

  "Deserializer" should "support nested inner classes as values" in {
    val input = Dog("Smurf", thinking = true)
    val json = mapper.writeValueAsString(input)
    val output = deserialize[Dog](json)

    output.name should be === ("Smurf")
    output.brain should not be (null)
    output.brain.isThinking should be (true)
    output.brain.parentName should be === ("Smurf")
    output.name = "Foo"
    output.brain.parentName should be === ("Foo")
  }

}
