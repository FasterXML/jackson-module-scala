package com.fasterxml.jackson.module.scala.javadsl

import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerModule

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
  it should "support OptionModule" in {
    javadsl.ScalaModule.optionModule() shouldBe an[OptionModule]
  }
  it should "support SetModule" in {
    javadsl.ScalaModule.setModule() shouldBe a[SetModule]
  }
  it should "support MapModule" in {
    javadsl.ScalaModule.mapModule() shouldBe a[MapModule]
  }
  it should "support TupleModule" in {
    javadsl.ScalaModule.tupleModule() shouldBe a[TupleModule]
  }
  it should "support UntypedObjectDeserializerModule" in {
    javadsl.ScalaModule.untypedObjectDeserializerModule() shouldBe an[UntypedObjectDeserializerModule]
  }
}
