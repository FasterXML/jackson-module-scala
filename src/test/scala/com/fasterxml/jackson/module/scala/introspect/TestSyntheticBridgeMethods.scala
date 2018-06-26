package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.module.scala.BaseSpec
import org.scalatest.{Inside, LoneElement}

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
class TestSyntheticBridgeMethods extends BaseSpec with LoneElement with Inside {
  import TestSyntheticBridgeMethods._

  behavior of "BeanIntrospector"

  it should "correctly find properties that have bridge or synthetic method overloads" in {
    val beanDesc = BeanIntrospector[Inner](classOf[Inner])

    inside (beanDesc.properties.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "prop"
      p shouldBe empty
      f shouldBe defined
      g shouldBe defined
      s shouldBe empty
    }
  }
}
