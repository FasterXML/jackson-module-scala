package com.fasterxml.jackson.module.scala.deser


import com.fasterxml.jackson.module.scala.JacksonModule

import com.fasterxml.jackson.databind.introspect.{AnnotatedField, AnnotatedConstructor, AnnotatedParameter, NopAnnotationIntrospector}

import com.fasterxml.jackson.module.scala.reflect.{MethodSignature, BeanMirror}

private object CaseClassAnnotationIntrospector extends NopAnnotationIntrospector {
  override def findDeserializationName(af: AnnotatedField): String = {
    try {
      if (BeanMirror(af.getDeclaringClass).hasSetter(af.getName, af.getRawType)) "" else null
    } catch {
      case _: IllegalArgumentException => null
    }
  }

  override def findDeserializationName(param: AnnotatedParameter): String = {
    param.getOwner match {
      case constructor: AnnotatedConstructor => findConstructorParamName(constructor, param)
      case _ => null
    }
  }

  private def findConstructorParamName(constructor: AnnotatedConstructor, param: AnnotatedParameter): String = {
    try {
      BeanMirror(constructor.getDeclaringClass).
        getConstructorParameterName(MethodSignature(constructor), param.getIndex) match {
        case Some(name) => name
        case None => null
      }
    } catch {
      case _: IllegalArgumentException => null
    }
  }
}


trait CaseClassDeserializerModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(CaseClassAnnotationIntrospector) }
}