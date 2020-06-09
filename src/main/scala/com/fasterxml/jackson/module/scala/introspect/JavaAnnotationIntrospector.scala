package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Constructor, Field, Method, Parameter}

import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.introspect.{Annotated, AnnotatedMember, AnnotatedParameter, NopAnnotationIntrospector}

object JavaAnnotationIntrospector extends NopAnnotationIntrospector {

  def findNameForDeserialization(a: Annotated): PropertyName = None.orNull

  def findImplicitPropertyName(param: AnnotatedMember): String = param match {
    case param: AnnotatedParameter => {
      val index = param.getIndex
      val owner = param.getOwner
      owner.getAnnotated match {
        case ctor: Constructor[_] => {
          val names = JavaParameterIntrospector.getCtorParamNames(ctor)
          if (index < names.length) names(index) else None.orNull
        }
        case method: Method => {
          val names = JavaParameterIntrospector.getMethodParamNames(method)
          if (index < names.length) names(index) else None.orNull
        }
        case field: Field => JavaParameterIntrospector.getFieldName(field)
        case parameter: Parameter => JavaParameterIntrospector.getParameterName(parameter)
        case _ => None.orNull
      }
    }
    case _ => None.orNull
  }
}
