package com.fasterxml.jackson.module.scala.util

import com.fasterxml.jackson.module.scala.{BaseSpec, Weekday}

class TastyUtilTest extends BaseSpec {

  "TastyUtil.hasTastyFile" should "support EnumResolver (class)" in {
    TastyUtil.hasTastyFile(classOf[EnumResolver]) shouldBe true
  }
  it should "support TastyUtil (object)" in {
    TastyUtil.hasTastyFile(TastyUtil.getClass) shouldBe true
  }
  it should "support Weekday (scala2 enum)" in {
    TastyUtil.hasTastyFile(Weekday.getClass) shouldBe true
  }
  it should "support ColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(ColorEnum.getClass) shouldBe true
  }
  it should "support JavaCompatibleColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(JavaCompatibleColorEnum.getClass) shouldBe true
  }
  it should "support EnclosingObject.EnclosedColorEnum (scala3 enum)" in {
    TastyUtil.hasTastyFile(EnclosingObject.EnclosedColorEnum.getClass) shouldBe true
  }
  it should "support EnclosingObject.EnclosedScala3Class (scala3 enum)" in {
    TastyUtil.hasTastyFile(classOf[EnclosingObject.EnclosedScala3Class]) shouldBe true
  }
  it should "not support Java class" in {
    TastyUtil.hasTastyFile(classOf[String]) shouldBe false
  }
}
