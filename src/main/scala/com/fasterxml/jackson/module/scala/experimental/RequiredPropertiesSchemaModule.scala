package com.fasterxml.jackson.module.scala.experimental

import com.fasterxml.jackson.databind.introspect.{AnnotatedMember, NopAnnotationIntrospector}

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector}
 */
@Deprecated
object DefaultRequiredAnnotationIntrospector extends NopAnnotationIntrospector {
  override def hasRequiredMarker(m: AnnotatedMember) =
    com.fasterxml.jackson.module.scala.DefaultRequiredAnnotationIntrospector.hasRequiredMarker(m)

}

/**
 * @deprecated use {@link com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule}
 */
@Deprecated
trait RequiredPropertiesSchemaModule extends com.fasterxml.jackson.module.scala.RequiredPropertiesSchemaModule
