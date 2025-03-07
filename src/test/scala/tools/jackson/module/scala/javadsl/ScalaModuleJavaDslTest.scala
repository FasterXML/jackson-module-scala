package tools.jackson.module.scala.javadsl

import tools.jackson.module.scala._

class ScalaModuleJavaDslTest extends BaseSpec {
  "Java DSL ScalaModule" should "support EnumerationModule" in {
    javadsl.ScalaModule.enumerationModule() shouldBe an[EnumerationModule]
  }
  it should "support EitherModule" in {
    javadsl.ScalaModule.eitherModule() shouldBe an[EitherModule]
  }
  it should "support IterableModule" in {
    javadsl.ScalaModule.iterableModule() shouldBe an[IterableModule]
  }
  it should "support IteratorModule" in {
    javadsl.ScalaModule.iteratorModule() shouldBe an[IteratorModule]
  }
}
