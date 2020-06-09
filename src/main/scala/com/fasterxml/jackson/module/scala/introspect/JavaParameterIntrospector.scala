package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Constructor, Field, Method, Parameter}

import com.thoughtworks.paranamer.{BytecodeReadingParanamer, CachingParanamer}

import scala.util.Try

private[introspect] object JavaParameterIntrospector {

  private val paranamer = new CachingParanamer(new BytecodeReadingParanamer)

  def getCtorParamNames(ctor: Constructor[_]): IndexedSeq[String] = {
    //val reflectNames = Try(ctor.getParameters.map(_.getName).toIndexedSeq).getOrElse(IndexedSeq.empty)
    //if (reflectNames.isEmpty || isAnyBlank(reflectNames)) {
      paranamer.lookupParameterNames(ctor).toIndexedSeq
    //} else {
      //reflectNames
    //}
  }

  def getMethodParamNames(mtd: Method): IndexedSeq[String] = {
    //val reflectNames = Try(mtd.getParameters.map(_.getName).toIndexedSeq).getOrElse(IndexedSeq.empty)
    //if (reflectNames.isEmpty || isAnyBlank(reflectNames)) {
      paranamer.lookupParameterNames(mtd).toIndexedSeq
    //} else {
      //reflectNames
    //}
  }

  def getFieldName(field: Field): String = {
    //val name = field.getName
    //if (isBlank(name)) {
      paranamer.lookupParameterNames(field).headOption.getOrElse(None.orNull)
    //} else {
      //name
    //}
  }

  def getParameterName(parameter: Parameter): String = parameter.getName

  private def isAnyBlank(css: Seq[String]): Boolean = {
    !css.forall(isNotBlank)
  }

  private def isNotBlank(cs: String): Boolean = !isBlank(cs)

  private def isBlank(cs: String): Boolean = {
    val strLen = length(cs)
    if (strLen == 0) {
      true
    } else {
      for (i <- 0 until strLen) {
        if (!Character.isWhitespace(cs.charAt(i))) return false
      }
      true
    }
  }

  private def length(cs: String): Int = Option(cs) match {
    case Some(s) => s.length
    case _ => 0
  }
}
