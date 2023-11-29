package com.fasterxml.jackson.module.scala.util

import java.lang.reflect.Field

object ScalaObject {

  private val MODULE_FIELD_NAME = "MODULE$"

  private def getStaticField(field: Field): Option[Any] =
    try Some(field.get(null))
    catch {
      case _: NullPointerException => None
    }

  private def moduleFieldOption(clazz: Class[_]): Option[Field] =
    try Some(clazz.getDeclaredField(MODULE_FIELD_NAME))
    catch {
      case _: NoSuchFieldException => None
    }

  private def moduleFieldValue(clazz: Class[_]): Option[Any] = for {
    moduleField <- moduleFieldOption(clazz)
    value <- getStaticField(moduleField)
  } yield value

  def unapply(clazz: Class[_]): Option[Any] =
    if (clazz.getSimpleName.endsWith("$")) moduleFieldValue(clazz)
    else None
}
