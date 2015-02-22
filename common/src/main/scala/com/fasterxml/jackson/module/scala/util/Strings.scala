package com.fasterxml.jackson.module.scala.util

import scala.language.implicitConversions

trait StringW extends PimpedType[String] {
  def orIfEmpty(s2: => String) = if (value.isEmpty) s2 else value
}

object StringW {
  def apply(s: => String): StringW = new StringW {
    lazy val value = s
  }
  def unapply(s: StringW): Option[String] = Some(s.value)
}

trait Strings {
  implicit def mkStringW(x: => String): StringW = StringW(x)
  implicit def unMkStringW(x: StringW): String = x.value
}
