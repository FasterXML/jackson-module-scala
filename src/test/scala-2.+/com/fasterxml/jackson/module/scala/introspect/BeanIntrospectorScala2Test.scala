package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.module.scala.BaseSpec
import com.fasterxml.jackson.module.scala.introspect.BeanIntrospectorTest.DecodedNameMatcher
import org.scalatest.{Inside, LoneElement, OptionValues}

// see also BeanIntrospectorTest for tests that also pass with Scala3
class BeanIntrospectorScala2Test extends BaseSpec with Inside with LoneElement with OptionValues with DecodedNameMatcher {

  behavior of "BeanIntrospector"

  it should "recognize a private [this] val field" in {

    //see equivalent test in BeanIntrospectorTest
    class Bean {
      private [this] val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g shouldBe empty
      s shouldBe empty
    }
  }

  it should "recognize a val field" in {

    //see equivalent test in BeanIntrospectorTest
    class Bean {
      private val `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s shouldBe empty
    }
  }

  it should "recognize a var field" in {

    //see equivalent test in BeanIntrospectorTest
    class Bean {
      private var `field-name` = 0
    }

    val beanDesc = BeanIntrospector[Bean](classOf[Bean])
    val props = beanDesc.properties

    inside (props.loneElement) { case PropertyDescriptor(n,p,f,g,s,_,_) =>
      n shouldBe "field-name"
      p shouldBe empty
      f.value should have (decodedName ("field-name"))
      g.value should have (decodedName ("field-name"))
      s.value should have (decodedName ("field-name_="))
    }
  }
}
