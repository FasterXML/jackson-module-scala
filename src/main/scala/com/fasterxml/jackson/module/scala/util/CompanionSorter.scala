package com.fasterxml.jackson.module.scala.util

import scala.collection.IterableFactory
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * The CompanionSorter performs a topological sort on class hierarchy to ensure that the most specific builders
 * get registered first.
 */
class CompanionSorter[CC[X] <: Iterable[X]] {
  type HKClassManifest[CC2[_]] = ClassTag[CC2[_]]

  private[this] val companions = new ArrayBuffer[(Class[_], IterableFactory[CC])]()

  def add[T[X] <: CC[X] : HKClassManifest](companion: IterableFactory[T]): CompanionSorter[CC] = {
    companions += implicitly[HKClassManifest[T]].runtimeClass -> companion
    this
  }

  def toList: List[(Class[_], IterableFactory[CC])] = {
    val cs = companions.toArray
    val output = new ListBuffer[(Class[_], IterableFactory[CC])]()

    val remaining = cs.map(_ => 1)
    val adjMatrix = Array.ofDim[Int](cs.length, cs.length)

    // Build the adjacency matrix. Only mark the in-edges.
    for (i <- cs.indices; j <- cs.indices) {
      val (ic, _) = cs(i)
      val (jc, _) = cs(j)

      if (i != j && ic.isAssignableFrom(jc)) {
        adjMatrix(i)(j) = 1
      }
    }

    // While we haven't removed every node, remove all nodes with 0 degree in-edges.
    while (output.length < cs.length) {
      val startLength = output.length

      for (i <- cs.indices) {
        if (remaining(i) == 1 && dotProduct(adjMatrix(i), remaining) == 0) {
          output += companions(i)
          remaining(i) = 0
        }
      }

      // If we couldn't remove any nodes, it means we've found a cycle. Realistically this should never happen.
      if (output.length == startLength) {
        throw new IllegalStateException("Companions contain a cycle.")
      }
    }

    output.toList
  }

  private[this] def dotProduct(a: Array[Int], b: Array[Int]): Int = {
    if (a.length != b.length) throw new IllegalArgumentException()

    a.indices.map(i => a(i) * b(i)).sum
  }
}
