package com.fasterxml.jackson.module.scala

class ScalaModule3Test extends BaseSpec {
  "A ScalaModule builder" should "support addAllBuiltinModules" in {
    val cfg = ScalaModule.builder().addAllBuiltinModules()
    // ScalaModuleTest tests the modules that work in Scala 2 and 3
    // here we test the Scala3 specific classes
    cfg.hasModule(EnumModule) shouldBe true
  }
}
