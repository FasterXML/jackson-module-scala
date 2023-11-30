package com.fasterxml.jackson.module.scala.util

import org.scalatest.matchers.should.Matchers.{contain, convertToAnyShouldWrapper, empty}
import org.scalatest.wordspec.AnyWordSpecLike

object TestObject

case object TestCaseObject

class TestClass

class TestClassWithModuleField {
  val MODULE$: TestClassWithModuleField = this
}

object BarWrapper {
  object Bar {
    final case class Baz()
  }
}

class ScalaObjectTest extends AnyWordSpecLike {

  "ScalaObject" must {
    "return Some(TestObject) for unapply(TestObject.getClass)" in {
      ScalaObject.unapply(TestObject.getClass) should contain(TestObject)
    }

    "return Some(TestCaseObject) for unapply(TestCaseObject.getClass)" in {
      ScalaObject.unapply(TestCaseObject.getClass) should contain(TestCaseObject)
    }

    "return None for unapply(testClassInstance.getClass)" in {
      val testClassInstance = new TestClass
      ScalaObject.unapply(testClassInstance.getClass) shouldBe empty
    }

    "return None for unapply(testClassWithModuleFieldInstance.getClass)" in {
      val testClassWithModuleFieldInstance = new TestClassWithModuleField
      ScalaObject.unapply(testClassWithModuleFieldInstance.getClass) shouldBe empty
    }

    "return None for unapply(bazInstance.getClass)" in {
      val bazInstance = BarWrapper.Bar.Baz()
      ScalaObject.unapply(bazInstance.getClass) shouldBe empty
    }
  }
}
