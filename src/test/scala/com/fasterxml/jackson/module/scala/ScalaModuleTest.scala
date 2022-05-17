package com.fasterxml.jackson.module.scala

class ScalaModuleTest extends BaseSpec {
  "A ScalaModule builder" should "support enabling/disabling scala 3 support" in {
    val cfg = ScalaModule.builder()
    cfg.shouldSupportScala3Classes() shouldBe true
    cfg.supportScala3Classes(false)
      .shouldSupportScala3Classes() shouldBe false
    cfg.supportScala3Classes(true)
      .shouldSupportScala3Classes() shouldBe true
  }
  it should "support enabling/disabling default value support" in {
    val cfg = ScalaModule.builder()
    cfg.shouldApplyDefaultValuesWhenDeserializing() shouldBe true
    cfg.applyDefaultValuesWhenDeserializing(false)
      .shouldApplyDefaultValuesWhenDeserializing() shouldBe false
    cfg.applyDefaultValuesWhenDeserializing(true)
      .shouldApplyDefaultValuesWhenDeserializing()shouldBe true
  }
}
