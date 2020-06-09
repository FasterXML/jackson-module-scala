package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Constructor, Field, Method, Parameter}

private[introspect] object JavaParameterIntrospector {

  def getCtorParamNames(ctor: Constructor[_]): IndexedSeq[String] = {
    ctor.getParameters.map(_.getName).toIndexedSeq
  }

  def getMethodParamNames(mtd: Method): IndexedSeq[String] = {
    mtd.getParameters.map(_.getName).toIndexedSeq
  }

  def getFieldName(field: Field): String = field.getName

  def getParameterName(parameter: Parameter): String = parameter.getName
}
