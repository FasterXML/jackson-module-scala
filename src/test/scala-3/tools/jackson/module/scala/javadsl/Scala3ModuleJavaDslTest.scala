package tools.jackson.module.scala.javadsl

import tools.jackson.module.scala.{ BaseSpec, EnumModule }

class ScalaModule3Test extends BaseSpec {
  "Java DSL ScalaModule" should "support EnumModule (Scala 3 only)" in {
    val module = ScalaModule.enumModule()
    module shouldBe an[EnumModule]
  }
}
