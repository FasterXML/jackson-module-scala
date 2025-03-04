package tools.jackson.module.scala

class JavaDslTest extends BaseSpec {
  "Java DSL" should "support getting DefaultScalaModule" in {
    javadsl.DefaultScalaModule.getInstance() shouldBe DefaultScalaModule
  }
  it should "support ScalaModule builder" in {
    val builder = javadsl.ScalaModule.builder().addAllBuiltinModules()
    builder.hasModule(IteratorModule) shouldBe true
  }
}
