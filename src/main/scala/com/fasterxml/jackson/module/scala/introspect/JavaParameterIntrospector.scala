package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Constructor, Field, Method, Parameter}

import com.thoughtworks.paranamer.{BytecodeReadingParanamer, CachingParanamer}

private[introspect] object JavaParameterIntrospector {

  private val paranamer = new CachingParanamer(new BytecodeReadingParanamer)

  def getCtorParamNames(ctor: Constructor[_]): IndexedSeq[String] = {
    paranamer.lookupParameterNames(ctor, false).toIndexedSeq
  }

  def getMethodParamNames(mtd: Method): IndexedSeq[String] = {
    paranamer.lookupParameterNames(mtd, false).toIndexedSeq
  }

  def getFieldName(field: Field): String = {
    paranamer.lookupParameterNames(field, false).headOption.getOrElse(None.orNull)
  }

  def getParameterName(parameter: Parameter): String = parameter.getName
}
