package com.fasterxml.jackson.module.scala

import scala.collection.generic.{GenericCompanion, SortedSetFactory}
import scala.collection.{GenTraversable, GenTraversableOnce, SortedSet, SortedSetLike}
import scala.language.higherKinds

/**
  * Here we add some type aliases for things that were moved or reworked in scala 2.13.
  */
package object deser {
  type IterableFactory[+CC[X] <: GenTraversable[X]] = GenericCompanion[CC]
  type SortedIterableFactory[CC[A] <: SortedSet[A] with SortedSetLike[A, CC[A]]] = SortedSetFactory[CC]

  type LazyList[+A] = Stream[A]
  val LazyList: Nil.type = Nil

  // added in 2.13
  type ArrayDeque[+A] = Iterable[A]
  val ArrayDeque: Nil.type = Nil
}
