package com.fasterxml.jackson.module.external

import com.fasterxml.jackson.module.scala.BaseSpec

class CustomScalaModuleTest extends BaseSpec {

    "A custom scala module" should "be buildable outside of the module package" in {
        """
          |
          |import com.fasterxml.jackson.module.scala._
          |import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, UntypedObjectDeserializerModule}
          |import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
          |
          |class CustomScalaModule
          |  extends JacksonModule
          |     with IteratorModule
          |     with EnumerationModule
          |     with OptionModule
          |     with SeqModule
          |     with IterableModule
          |     with TupleModule
          |     with MapModule
          |     with SetModule
          |     with ScalaNumberDeserializersModule
          |     with ScalaAnnotationIntrospectorModule
          |     with UntypedObjectDeserializerModule
          |     with EitherModule
          |
        """.stripMargin should compile
    }
}
