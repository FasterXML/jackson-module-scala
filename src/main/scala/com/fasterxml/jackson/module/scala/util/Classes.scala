package com.fasterxml.jackson.module.scala.util

import java.lang.reflect.Field
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.reflect.{ScalaLongSignature, ScalaSignature}
import scala.util.Try

trait ClassW extends PimpedType[Class[_]] {

  @deprecated("use extendsScalaClass(Boolean) instead", "2.14.0")
  def extendsScalaClass: Boolean = extendsScalaClass(true)

  def extendsScalaClass(supportScala3Classes: Boolean): Boolean = {
    ClassW.productClass.isAssignableFrom(value) ||
      isScalaObject ||
      (supportScala3Classes && TastyUtil.hasTastyFile(value))
  }

  def hasSignature: Boolean = {
    @tailrec
    def hasSigHelper(clazz: Class[_]): Boolean = {
      if (clazz == null) false
      else if (clazz.isAnnotationPresent(classOf[ScalaSignature])
        || clazz.isAnnotationPresent(classOf[ScalaLongSignature])) true
      //if the class does not have the signature, check it's enclosing class (if present)
      else hasSigHelper(clazz.getEnclosingClass)
    }
    hasSigHelper(value)
  }

  def isScalaObject: Boolean = moduleField.isSuccess

  def getModuleField: Option[Field] = moduleField.toOption

  private lazy val moduleField: Try[Field] = Try(value.getField("MODULE$"))
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
