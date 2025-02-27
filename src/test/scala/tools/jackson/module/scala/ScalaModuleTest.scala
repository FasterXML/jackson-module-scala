package tools.jackson.module.scala

import tools.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import tools.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

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
  it should "support addAllBuiltinModules" in {
    val cfg = ScalaModule.builder().addAllBuiltinModules()
    cfg.hasModule(IteratorModule) shouldBe true
    cfg.hasModule(IterableModule) shouldBe true
    cfg.hasModule(EnumerationModule) shouldBe true
    cfg.hasModule(EitherModule) shouldBe true
    cfg.hasModule(OptionModule) shouldBe true
    cfg.hasModule(MapModule) shouldBe true
    cfg.hasModule(SeqModule) shouldBe true
    cfg.hasModule(SetModule) shouldBe true
    cfg.hasModule(TupleModule) shouldBe true
    cfg.hasModule(ScalaAnnotationIntrospectorModule) shouldBe true
    cfg.hasModule(ScalaNumberDeserializersModule) shouldBe true
    cfg.hasModule(ScalaObjectDeserializerModule) shouldBe true
    cfg.hasModule(SymbolModule) shouldBe true
    cfg.hasModule(UntypedObjectDeserializerModule) shouldBe true
  }
}
