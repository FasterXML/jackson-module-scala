package com.fasterxml.jackson.module.scala

import scala.language.implicitConversions

/**
  * Here we add some type aliases for things that were moved or reworked in scala 2.13.
  */
package object ser {
  class PimpedIterator[+A](iter: Iterator[A]) {
    def knownSize: Int = if (iter.hasDefiniteSize) iter.size else 0
  }
  class PimpedIterable[+A](iter: Iterable[A]) {
    def knownSize: Int = if (iter.hasDefiniteSize) iter.size else 0
  }

  implicit def pimpThisIterator[A](iter: Iterator[A]): PimpedIterator[A] = new PimpedIterator[A](iter)
  implicit def pimpThisIterable[A](iter: Iterable[A]): PimpedIterable[A] = new PimpedIterable[A](iter)
}
