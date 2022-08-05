package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}

/**
 * @deprecated use [[com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector]]
 */
@deprecated("use com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector", "2.12.1")
object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {
  override def hasRequiredMarker(m: AnnotatedMember) =
    com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.hasRequiredMarker(m)

}

/**
 * @deprecated use [[com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule]]
 */
@deprecated("use com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule", "2.12.1")
trait RequiredPropertiesSchemaModule extends com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule
