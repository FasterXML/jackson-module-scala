package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, ScalaObjectDeserializerModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

class ScalaModuleTest extends BaseSpec {
  "A ScalaModule builder" should "support addAllBuiltinModules" in {
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
  it should "support removeModule" in {
    val builder = ScalaModule.builder()
      .addAllBuiltinModules()
      .removeModule(SymbolModule)

    builder.hasModule(UntypedObjectDeserializerModule) shouldBe true
    builder.hasModule(SymbolModule) shouldBe false

    builder.build() shouldBe a[JacksonModule]
  }
  it should "support registering custom module in JsonMapper" in {
    val builder = ScalaModule.builder()
      .addAllBuiltinModules()
    val mapper = JsonMapper.builder()
      .addModule(builder.build())
      .build()
    val result = mapper.writeValueAsString(Symbol("symbol"))
    result should be (""""symbol"""")
  }
}
