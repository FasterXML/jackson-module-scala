package com.fasterxml.jackson.module.external

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala._
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

  "A custom scala module" should "be buildable outside of the module package" in {
    val builder = JsonMapper.builder().addModule(new CustomScalaModule)
    val mapper = builder.build()
    val testInstance = CustomScalaModuleTest.TestClass(BigDecimal("1.23"), Map("key" -> "value"))
    val text = mapper.writeValueAsString(testInstance)
    mapper.readValue(text, classOf[CustomScalaModuleTest.TestClass]) shouldEqual testInstance
  }
}
