package com.fasterxml.jackson.module.scala

import scala.collection.{concurrent, mutable}

/**
  * Here we add some type aliases for things that were removed or reworked in scala 2.13.
  */
package object deser {
  object overrides {
    // Added in 2.13
    type ArrayDeque[A] = mutable.ArrayDeque[A]
    type TrieMap[A, B] = concurrent.TrieMap[A, B]

    // Mutable versions of these were added in 2.12
    type SortedMap[A, B] = mutable.SortedMap[A, B]
    type TreeMap[A, B] = mutable.TreeMap[A, B]

    // Removed in 2.13
    type MutableList[+A] = Iterable[A]
    type ResizableArray[+A] = Iterable[A]
  }
}
