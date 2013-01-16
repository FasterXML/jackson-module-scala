package com.fasterxml.jackson.module.scala.util

import scala.reflect.{ScalaLongSignature, ScalaSignature}

trait ClassW extends PimpedType[Class[_]] {
  def hasSignature: Boolean = {
    val sig = Option(value.getAnnotation(classOf[ScalaSignature]))
    lazy val longSig = Option(value.getAnnotation(classOf[ScalaLongSignature]))
    (sig orElse longSig).isDefined
  }
}

object ClassW {
  def apply(c: => Class[_]): ClassW = new ClassW {
    lazy val value = c
  }
  def unapply(c: ClassW): Option[Class[_]] = Some(c.value)
}

trait Classes {
  implicit def mkClassW(x: => Class[_]): ClassW = ClassW(x)
  implicit def unMkClassW[A](x: ClassW): Class[_] = x.value
}
