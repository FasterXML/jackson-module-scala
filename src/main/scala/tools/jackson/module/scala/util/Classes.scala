package tools.jackson.module.scala.util

import java.lang.reflect.Field
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.reflect.{ScalaLongSignature, ScalaSignature}
import scala.util.Try

trait ClassW extends PimpedType[Class[_]] {

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

  private val ModuleFieldName = "MODULE$"

  /**
   * The loader to look a class up beside `clazz` with: its own, or the system loader for a class the
   * bootstrap loader defined, which reports none.
   */
  def loaderFor(clazz: Class[_]): ClassLoader =
    Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)

  /**
   * The class of the companion object of the class named `className`, loaded by `loader` but not
   * initialized. Throws as `Class.forName` does when there is none.
   */
  def companionClassNamed(className: String, loader: ClassLoader): Class[_] =
    Class.forName(className + "$", false, loader)

  /** The class of `clazz`'s companion object, if it has one, loaded but not initialized. */
  def companionClassOf(clazz: Class[_]): Option[Class[_]] =
    Try(companionClassNamed(clazz.getName, loaderFor(clazz))).toOption

  /** The companion object of `clazz`, if it has one. Loading it initializes it. */
  def companionOf(clazz: Class[_]): Option[AnyRef] =
    companionClassOf(clazz).flatMap(companion => Try(companion.getField(ModuleFieldName).get(None.orNull)).toOption)

  def apply(c: => Class[_]): ClassW = new ClassW {
    lazy val value = c
  }
  def unapply(c: ClassW): Option[Class[_]] = Some(c.value)
}

trait Classes {
  implicit def mkClassW(x: => Class[_]): ClassW = ClassW(x)
  implicit def unMkClassW[A](x: ClassW): Class[_] = x.value
}
