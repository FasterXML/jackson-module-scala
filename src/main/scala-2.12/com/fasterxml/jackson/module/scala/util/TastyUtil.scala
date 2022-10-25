package com.fasterxml.jackson.module.scala.util

private[util] object TastyUtil {
  // this check is not useful in Scala 2.11/2.12
  def hasTastyFile(clz: Class[_]): Boolean = false
}
