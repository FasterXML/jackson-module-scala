package com.fasterxml.jackson.module.scala.introspect

import org.scalatest.{FlatSpec, ShouldMatchers}

object TestSyntheticBridgeMethods {
  trait Outer {
    def prop: Option[String]
  }

  class Inner extends Outer {
    val prop = Some("prop_value")
  }
}

// Note that the behavior being tested here was previously depending on
// undefined ordering of methods from JVM reflection. This test should
// consistently pass now, but previously it could alternately pass or
// fail depending on the order used by the JVM for that execution.
class TestSyntheticBridgeMethods extends FlatSpec with ShouldMatchers {
  import TestSyntheticBridgeMethods._

  behavior of "BeanIntrospector"

  it should "correctly find properties that have bridge or synthetic method overloads" in {
    val beanDesc = BeanIntrospector[Inner](classOf[Inner])
    beanDesc.properties should have length 1

    val propDesc = beanDesc.properties.head
    propDesc.name should be === "prop"
    propDesc.param should be ('empty)
    propDesc.field should be ('defined)
    propDesc.getter should be ('defined)
    propDesc.setter should be ('empty)
  }

}
