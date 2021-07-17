package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}

object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {

  private val OPTION = classOf[Option[_]]
  private val JSON_PROPERTY = classOf[JsonProperty]

  private def isOptionType(cls: Class[_]) = OPTION.isAssignableFrom(cls)

  override def hasRequiredMarker(config: MapperConfig[_], m: AnnotatedMember) = boolean2Boolean(
    Option(m.getAnnotation(JSON_PROPERTY)).map(_.required).getOrElse(!isOptionType(m.getRawType))
  )

}

trait RequiredPropertiesSchemaModule extends JacksonModule {
  this += { _.insertAnnotationIntrospector(DefaultRequiredAnnotationIntrospector) }
}

class RequiredPropertiesSchemaModuleInstance(override val config: ScalaModule.Config) extends RequiredPropertiesSchemaModule

object RequiredPropertiesSchemaModule extends RequiredPropertiesSchemaModule