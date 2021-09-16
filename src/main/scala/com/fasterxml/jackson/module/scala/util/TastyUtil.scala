package com.fasterxml.jackson.module.scala.util

import scala.annotation.tailrec

private[util] object TastyUtil {
  private val thisClass = TastyUtil.getClass
  
  @tailrec
  def hasTastyFile(clz: Class[_]): Boolean = {
    if (clz == null) {
      false
    } else {
      lazy val className = getClassName(clz)
      className != null && {
        val baseName = className.replace(".", "/")
        val classFileBase = if (baseName.endsWith("$")) {
          baseName.substring(0, baseName.length - 1)
        } else {
          baseName
        }
        val tastyFile = s"/$classFileBase.tasty"
        Option(thisClass.getResource(tastyFile)).isDefined
      } || hasTastyFile(clz.getEnclosingClass)
    }
  }

  private def getClassName(clz: Class[_]): String = {
    try clz.getCanonicalName catch {
      case _: InternalError => null
    }
  }
}
