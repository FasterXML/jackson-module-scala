package com.fasterxml.jackson.module.scala.introspect

import com.thoughtworks.paranamer.{BytecodeReadingParanamer, CachingParanamer}

import java.lang.reflect.{Constructor, Field, Method, Parameter}
import scala.util.Try

private[introspect] object JavaParameterIntrospector {

  private val paranamer = new CachingParanamer(new BytecodeReadingParanamer)

  def getCtorParamNames(ctor: Constructor[_]): IndexedSeq[String] = {
    Try(paranamer.lookupParameterNames(ctor, false))
      .getOrElse(ctor.getParameters.map(_.getName))
      .toIndexedSeq
  }

  def getMethodParamNames(mtd: Method): IndexedSeq[String] = {
    Try(paranamer.lookupParameterNames(mtd, false))
      .getOrElse(mtd.getParameters.map(_.getName))
      .toIndexedSeq
  }

  def getFieldName(field: Field): String = {
    Try(paranamer.lookupParameterNames(field, false).headOption.getOrElse(None.orNull))
      .getOrElse(field.getName)
  }

  def getParameterName(parameter: Parameter): String = parameter.getName
}
