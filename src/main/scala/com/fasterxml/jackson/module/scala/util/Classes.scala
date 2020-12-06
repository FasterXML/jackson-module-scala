package com.fasterxml.jackson.module.scala.util

import scala.language.implicitConversions
import scala.reflect.{ScalaLongSignature, ScalaSignature}

trait ClassW extends PimpedType[Class[_]] {

  def extendsScalaClass: Boolean = {
    ClassW.productClass.isAssignableFrom(value)
  }

  def hasSignature: Boolean = {
    def hasSigHelper(clazz: Class[_]): Boolean = {
      if (clazz == null) false
      else if (clazz.isAnnotationPresent(classOf[ScalaSignature])
        || clazz.isAnnotationPresent(classOf[ScalaLongSignature])) true
      //if the class does not have the signature, check it's enclosing class (if present)
      else hasSigHelper(clazz.getEnclosingClass)
    }
    hasSigHelper(value)
  }
}

object ClassW {
  val productClass = classOf[Product]

  def apply(c: => Class[_]): ClassW = new ClassW {
    lazy val value = c
  }
  def unapply(c: ClassW): Option[Class[_]] = Some(c.value)
}

trait Classes {
  implicit def mkClassW(x: => Class[_]): ClassW = ClassW(x)
  implicit def unMkClassW[A](x: ClassW): Class[_] = x.value
}
