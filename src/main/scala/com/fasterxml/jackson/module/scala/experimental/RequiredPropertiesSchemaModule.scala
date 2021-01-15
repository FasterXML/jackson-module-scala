package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector}
 */
@deprecated("use com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector", "2.10.0")
object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {
  override def hasRequiredMarker(m: AnnotatedMember) =
    com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.hasRequiredMarker(m)

}

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule}
 */
@deprecated("use com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule", "2.10.0")
trait RequiredPropertiesSchemaModule extends com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule
