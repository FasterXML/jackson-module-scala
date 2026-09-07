package tools.jackson.module.scala.util

import scala.annotation.tailrec

private[util] object TastyUtil {

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
        // asked of the class being checked, not of this one: the two share a classloader only when
        // the application's classes were loaded by the same one as the module's. Where they were
        // not - a container, OSGi, a script engine - this module's loader cannot see the .tasty of
        // a class it did not load, and every such class would be taken for a Java class.
        Option(clz.getResource(tastyFile)).isDefined
      } || hasTastyFile(clz.getEnclosingClass)
    }
  }

  private def getClassName(clz: Class[_]): String = {
    try clz.getCanonicalName catch {
      case _: InternalError => null
    }
  }
}
