package com.fasterxml.jackson.module.scala

/**
  * Here we add some type aliases for things that were removed or reworked in scala 2.13.
  */
package object deser {
  // Removed in 2.13
  type MutableList[+A] = Iterable[A]
  type ResizableArray[+A] = Iterable[A]
}
