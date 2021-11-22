package com.fasterxml.jackson.module.external

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.OptionWithNumberDeserializerTest.OptionLong
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object CustomScalaModuleTest {
  case class TestClass(decimal: BigDecimal, map: Map[String, String])
}

class CustomScalaModuleTest extends BaseSpec {

  class CustomScalaModule
    extends JacksonModule
      with IteratorModule
      with EnumerationModule
      with OptionModule
      with SeqModule
      with IterableModule
      with TupleModule
      with MapModule
      with SetModule
      with ScalaNumberDeserializersModule
      with ScalaAnnotationIntrospectorModule
      with UntypedObjectDeserializerModule
      with EitherModule

  //no longer works in jackson-module-scala 3 (use ScalaModule.builder instead)
  "A custom scala module" should "be buildable outside of the module package" ignore {
    val builder = JsonMapper.builder().addModule(new CustomScalaModule)
    val mapper = builder.build()
    val testInstance = CustomScalaModuleTest.TestClass(BigDecimal("1.23"), Map("key" -> "value"))
    val text = mapper.writeValueAsString(testInstance)
    mapper.readValue(text, classOf[CustomScalaModuleTest.TestClass]) shouldEqual testInstance
  }

  "A custom scala module" should "be buildable outside of the module package (ScalaModule.builder)" in {
    val scalaModule = ScalaModule.builder()
      .addModule(IteratorModule)
      .addModule(EnumerationModule)
      .addModule(OptionModule)
      .addModule(SeqModule)
      .addModule(IterableModule)
      .addModule(TupleModule)
      .addModule(MapModule)
      .addModule(SetModule)
      .addModule(ScalaNumberDeserializersModule)
      .addModule(ScalaAnnotationIntrospectorModule)
      .addModule(UntypedObjectDeserializerModule)
      .addModule(EitherModule)
      .build()
    val builder = JsonMapper.builder().addModule(scalaModule)
    val mapper = builder.build()
    val testInstance = CustomScalaModuleTest.TestClass(BigDecimal("1.23"), Map("key" -> "value"))
    val text = mapper.writeValueAsString(testInstance)
    mapper.readValue(text, classOf[CustomScalaModuleTest.TestClass]) shouldEqual testInstance
  }

  it should "deserialize OptionLong when registerReferencedValueType is used (custom scala module using ScalaAnnotationIntrospectorModule object)" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    try {
      val module = ScalaModule.builder().addModule(OptionModule).addModule(ScalaAnnotationIntrospectorModule)
      val builder = JsonMapper.builder().addModule(module.build())
      val mapper = builder.build()
      val v1 = mapper.readValue("""{"valueLong":151}""", classOf[OptionLong])
      v1 shouldBe OptionLong(Some(151L))
      v1.valueLong.get shouldBe 151L
      //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in OptionWithNumberDeserializerTest
      useOptionLong(v1.valueLong) shouldBe 302L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize OptionLong when registerReferencedValueType is used (custom scala module using custom ScalaAnnotationIntrospectorModule)" in {
    val introspectorModule = ScalaAnnotationIntrospectorModule.newStandaloneInstance()
    introspectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    introspectorModule.getRegisteredReferencedValueType(classOf[OptionLong], "valueLong") shouldEqual Some(classOf[Long])
    val module = ScalaModule.builder().addModule(OptionModule).addModule(introspectorModule)
    val builder = JsonMapper.builder().addModule(module.build())
    val mapper = builder.build()
    val v1 = mapper.readValue("""{"valueLong":151}""", classOf[OptionLong])
    v1 shouldBe OptionLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
    //or use one of the equivalent classes in OptionWithNumberDeserializerTest
    useOptionLong(v1.valueLong) shouldBe 302L

    //introspectorModule registrations should not affect ScalaAnnotationIntrospectorModule object
    ScalaAnnotationIntrospectorModule.getRegisteredReferencedValueType(classOf[OptionLong], "valueLong") shouldBe empty

    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    try {
      val introspectorModule = ScalaAnnotationIntrospectorModule.newStandaloneInstance()
      introspectorModule.getRegisteredReferencedValueType(classOf[OptionLong], "valueLong") shouldBe empty
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)

}
