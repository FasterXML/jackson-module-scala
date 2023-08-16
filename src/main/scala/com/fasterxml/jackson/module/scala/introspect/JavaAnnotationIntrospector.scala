package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.core.Version

import java.lang.reflect.{Constructor, Field, Method, Parameter}
import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.introspect.{Annotated, AnnotatedMember, AnnotatedParameter, NopAnnotationIntrospector}
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.reflect.NameTransformer

object JavaAnnotationIntrospector extends NopAnnotationIntrospector {

  override def findNameForDeserialization(a: Annotated): PropertyName = None.orNull

  override def findImplicitPropertyName(param: AnnotatedMember): String = {
    val result = param match {
      case param: AnnotatedParameter if ScalaAnnotationIntrospector.isMaybeScalaBeanType(param.getDeclaringClass) => {
        val index = param.getIndex
        val owner = param.getOwner
        owner.getAnnotated match {
          case ctor: Constructor[_] => {
            val names = JavaParameterIntrospector.getCtorParamNames(ctor)
            if (index < names.length) Option(names(index)) else None
          }
          case method: Method => {
            val names = JavaParameterIntrospector.getMethodParamNames(method)
            if (index < names.length) Option(names(index)) else None
          }
          case field: Field => Option(JavaParameterIntrospector.getFieldName(field))
          case parameter: Parameter => Option(JavaParameterIntrospector.getParameterName(parameter))
          case _ => None
        }
      }
      case _ => None
    }
    result.map(NameTransformer.decode).getOrElse(None.orNull)
  }

  override def version(): Version = JacksonModule.version
}
