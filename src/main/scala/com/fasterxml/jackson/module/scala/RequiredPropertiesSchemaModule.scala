package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder

object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {

  private val OPTION = classOf[Option[_]]
  private val JSON_PROPERTY = classOf[JsonProperty]

  private def isOptionType(cls: Class[_]) = OPTION.isAssignableFrom(cls)

  override def hasRequiredMarker(config: MapperConfig[_], m: AnnotatedMember) = boolean2Boolean(
    Option(m.getAnnotation(JSON_PROPERTY)).map(_.required).getOrElse(!isOptionType(m.getRawType))
  )

}

trait RequiredPropertiesSchemaModule extends JacksonModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += { _.insertAnnotationIntrospector(DefaultRequiredAnnotationIntrospector) }
    builder.build()
  }
}

object RequiredPropertiesSchemaModule extends RequiredPropertiesSchemaModule
