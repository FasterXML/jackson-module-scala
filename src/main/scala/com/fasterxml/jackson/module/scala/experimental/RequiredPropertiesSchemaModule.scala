package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}
import com.fasterxml.jackson.module.scala.JacksonModule

object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {

  private val OPTION = classOf[Option[_]]

  private def isOptionType(cls: Class[_]) = OPTION.isAssignableFrom(cls)

  override def hasRequiredMarker(m: AnnotatedMember) = boolean2Boolean(
    Option(m.getAnnotation(classOf[JsonProperty])).map(_.required).getOrElse(!isOptionType(m.getRawType))
  )

}

trait RequiredPropertiesSchemaModule extends JacksonModule {
  this += { _.insertAnnotationIntrospector(DefaultRequiredAnnotationIntrospector) }
}
