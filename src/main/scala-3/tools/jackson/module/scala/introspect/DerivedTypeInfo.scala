package tools.jackson.module.scala.introspect

import tools.jackson.module.scala.ScalaTypeInfo

import scala.util.Try

/**
 * Reads what a class captured by deriving [[ScalaTypeInfo]].
 *
 * A `derives` leaves nothing on the class itself - no parent, no annotation - so the companion is
 * what marks it: the compiler puts a `derived$ScalaTypeInfo` there, and that is the only trace of it
 * at runtime.
 */
private[introspect] object DerivedTypeInfo {

  private val MethodName = "derived$" + classOf[ScalaTypeInfo[_]].getSimpleName

  def erasedTypeArguments(clazz: Class[_]): Seq[(String, Class[_])] = {
    Try {
      val loader = Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)
      val companion = Class.forName(clazz.getName + "$", false, loader)
      val instance = companion.getField("MODULE$").get(None.orNull)
      val derived = companion.getMethod(MethodName).invoke(instance).asInstanceOf[ScalaTypeInfo[_]]
      derived.erasedTypeArguments.map { case (field, argument) => (field, argument: Class[_]) }
    }.getOrElse(Seq.empty)
  }
}
