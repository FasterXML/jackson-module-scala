package com.fasterxml.jackson.module.scala.experimental.util

import reflect._
import scala.reflect.runtime.{currentMirror => cm}
import scala.reflect.runtime.universe._

/**
 * This is a helper object to set the default parameter values after deserialization is complete.
 * Defaults are only set when either:
 * 1. a field is absent/missing in JSON
 * 2. the field is null
 *
 * @author Venkat Sudheer Reddy Aedama <sudheer.univ@gmail.com>
 * @since 2.5.0
 */
object DefaultParameterValuesSetter {

  def apply[T: Manifest](tempObj: T): T = {
    val allFields = manifest[T].runtimeClass.getDeclaredFields.toList
    defaultsMap(tempObj.getClass).foldLeft(tempObj) { case (obj, (name: String, value: Any)) =>
      allFields.find(_.getName == name).foreach { f =>
        f.setAccessible(true)
        Option(f.get(obj)).fold(f.set(obj, value))(_ => ())
      }
      obj
    }
  }

  // These helpers are inspired from the answer given by som-snytt:
  // http://stackoverflow.com/questions/13812172/how-can-i-create-an-instance-of-a-case-class-with-constructor-arguments-with-no/13813000#13813000

  private def defaultsMap[A](clazz: Class[A]): Map[String, Any] = {
    val claas: ClassSymbol = cm classSymbol ClassTag(clazz).runtimeClass
    val modul: ModuleSymbol = claas.companionSymbol.asModule
    val im: InstanceMirror = cm reflect (cm reflectModule modul).instance
    default[A](im, "apply")
  }

  private def default[A](im: InstanceMirror, name: String): Map[String, Any] = {
    val at = newTermName(name)
    val ts = im.symbol.typeSignature
    val method = (ts member at).asMethod

    // either defarg or default val for type of p
    def valueFor(p: Symbol, i: Int): Any = {
      val defarg = ts member newTermName(s"$name$$default$$${i + 1}")
      if (defarg != NoSymbol) {
        (im reflectMethod defarg.asMethod)()
      } else {
        p.typeSignature match {
          case t if t =:= typeOf[String] => null
          case t if t =:= typeOf[Int] => 0
          case x => throw new IllegalArgumentException(x.toString)
        }
      }
    }
    val args = (for (ps <- method.paramss; p <- ps) yield p).zipWithIndex map (p => p._1 -> valueFor(p._1, p._2))
    args.map { case (k, v) => k.name.decoded -> v}.toMap
  }

}
