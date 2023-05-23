package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}

@deprecated("will be removed in 3.0.0 release as jackson-module-jsonSchema is being discontinued", "2.13.0")
object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {

  private val OPTION = classOf[Option[_]]
  private val JSON_PROPERTY = classOf[JsonProperty]

  private def isOptionType(cls: Class[_]) = OPTION.isAssignableFrom(cls)

  override def hasRequiredMarker(m: AnnotatedMember) = boolean2Boolean(
    Option(m.getAnnotation(JSON_PROPERTY)).map(_.required).getOrElse(!isOptionType(m.getRawType))
  )

}

@deprecated("will be removed in 3.0.0 release as jackson-module-jsonSchema is being discontinued", "2.13.0")
trait RequiredPropertiesSchemaModule extends JacksonModule {
  override def getModuleName: String = "RequiredPropertiesSchemaModule"
  this += { _.insertAnnotationIntrospector(DefaultRequiredAnnotationIntrospector) }
}
