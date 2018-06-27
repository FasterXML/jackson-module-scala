package com.fasterxml.jackson.module.scala

import scala.collection.generic.{GenericCompanion, SortedSetFactory}
import scala.collection.{GenTraversable, SortedSet, SortedSetLike, immutable}
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

  // mutable versions of these were added in 2.12
  type SortedMap[A, +B] = immutable.SortedMap[A, B]
  type TreeMap[A, +B] = immutable.TreeMap[A, B]
}
