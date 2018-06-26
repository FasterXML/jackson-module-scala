package com.fasterxml.jackson.module.scala.util

import scala.language.implicitConversions

trait PimpedType[X] {
  def value: X
}

object PimpedType {
  implicit def UnwrapPimpedType[X](p: PimpedType[X]): X = p.value
}
