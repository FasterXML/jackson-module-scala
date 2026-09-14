package tools.jackson.module.scala.introspect

import tools.jackson.module.scala.util.ClassW

import java.lang.reflect.{Constructor, Field, Method, Modifier, Parameter}
import scala.util.Try

private[introspect] object JavaParameterIntrospector {

  def getCtorParamNames(ctor: Constructor[_]): IndexedSeq[String] = {
    ctor.getParameters.map(_.getName).toIndexedSeq
  }

  def getMethodParamNames(mtd: Method): IndexedSeq[String] = {
    val params = mtd.getParameters
    // a static forwarder compiled by Scala 3 carries no parameter names; the companion's method does
    val named = if (params.forall(_.isNamePresent)) params else companionMethod(mtd).map(_.getParameters).getOrElse(params)
    named.map(_.getName).toIndexedSeq
  }

  def getFieldName(field: Field): String = field.getName

  def getParameterName(parameter: Parameter): String = parameter.getName

  /**
   * The companion object method that a static forwarder on the class stands in for, if `mtd` is
   * one. scalac emits the forwarder so that Java sees the companion's methods as static; what it
   * copies onto the forwarder differs by compiler version, so anything it left behind is read from
   * the method it forwards to.
   */
  def companionMethod(mtd: Method): Option[Method] = {
    if (!Modifier.isStatic(mtd.getModifiers)) None
    else {
      ClassW.companionClassOf(mtd.getDeclaringClass)
        .flatMap(companion => Try(companion.getMethod(mtd.getName, mtd.getParameterTypes: _*)).toOption)
        .filterNot(m => Modifier.isStatic(m.getModifiers))
    }
  }
}
