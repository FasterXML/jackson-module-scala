package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

object ObjectSerializerTest {
  trait TraitWithoutJsonProperty {
    val name = "name1"
  }
  object ObjectWithoutJsonProperty extends TraitWithoutJsonProperty {
    val value = "value1"
  }
}

@RunWith(classOf[JUnitRunner])
class ObjectSerializerTest extends SerializerTest {

  import ObjectSerializerTest._
  def module = DefaultScalaModule

  "An ObjectMapper with the DefaultScalaModule" should "serialize an object as a bean" in {
    serialize(ObjectWithoutJsonProperty) should (
       equal ("""{"name":"name1","value":"value1"}""") or
       equal ("""{"value":"value1","name":"name1"}""")
    )
  }
}
