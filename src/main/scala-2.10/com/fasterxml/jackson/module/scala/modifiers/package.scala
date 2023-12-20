package com.fasterxml.jackson.module.scala

import scala.collection.{GenMap, GenTraversableOnce}

/**
  * Here we add some type aliases for things that were moved or reworked in scala 2.13.
  */
package object modifiers {
  type IterableOnce[+A] = GenTraversableOnce[A]
  type Map[A, B] = GenMap[A, B]
}
