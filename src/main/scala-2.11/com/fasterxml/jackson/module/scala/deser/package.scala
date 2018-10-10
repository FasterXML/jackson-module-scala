package com.fasterxml.jackson.module.scala

import scala.collection.generic.{GenericCompanion, SortedSetFactory}
import scala.collection.{GenTraversable, SortedSet, SortedSetLike, immutable, mutable}
import scala.language.higherKinds

/**
  * Here we add some type aliases for things that were moved or reworked in scala 2.13.
  */
package object deser {
  type IterableFactory[+CC[X] <: GenTraversable[X]] = GenericCompanion[CC]
  type SortedIterableFactory[CC[A] <: SortedSet[A] with SortedSetLike[A, CC[A]]] = SortedSetFactory[CC]

  object overrides {
    // Added in 2.13
    type ArrayDeque[+A] = Iterable[A]
    type LazyList[+A] = Stream[A]
    type TrieMap[A, B] = Map[A, B]

    // mutable versions of these were added in 2.12
    type SortedMap[A, +B] = immutable.SortedMap[A, B]
    type TreeMap[A, +B] = immutable.TreeMap[A, B]

    // Removed in 2.13
    type MutableList[A] = mutable.MutableList[A]
    type ResizableArray[A] = mutable.ResizableArray[A]
  }
}
