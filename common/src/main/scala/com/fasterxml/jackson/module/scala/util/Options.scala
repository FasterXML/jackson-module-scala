package com.fasterxml.jackson.module.scala.util

import scala.language.implicitConversions

trait OptionW[A] extends PimpedType[Option[A]] {
  def optMap[B](f: A => B): Option[B] =
    if (value.isEmpty) None else Option(f(value.get))
}

object OptionW {
  def apply[A](a: => Option[A]): OptionW[A] = new OptionW[A] {
    lazy val value = a
  }
  def unapply[A](v: OptionW[A]): Option[Option[A]] = Some(v.value)
}

trait Options {
  implicit def mkOptionW[A](x: Option[A]): OptionW[A] = OptionW(x)
  implicit def unMkOptionW[A](x: OptionW[A]): Option[A] = x.value
}
