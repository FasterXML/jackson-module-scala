package com.fasterxml.jackson.module.scala.util

import scala.reflect.{ScalaLongSignature, ScalaSignature}

trait ClassW extends PimpedType[Class[_]] {
  def hasSignature: Boolean = {
    def hasSigHelper(clazz: Class[_]): Boolean = {
      if(clazz == null) {
        false
      } else {
        val sig = Option(clazz.getAnnotation(classOf[ScalaSignature]))
        lazy val longSig = Option(clazz.getAnnotation(classOf[ScalaLongSignature]))
        if((sig orElse longSig).isDefined) {
          true
        } else {
          //if the class does not have the signature, check it's enclosing class (if present)
          hasSigHelper(clazz.getEnclosingClass)
        }
      }
    }
    hasSigHelper(value)
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
