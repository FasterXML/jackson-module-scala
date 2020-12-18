package com.fasterxml.jackson.module.scala.util

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.higherKinds
import scala.reflect.ClassTag

/**
 * The FactorySorter performs a topological sort on class hierarchy to ensure that the most specific builders
 * get registered first.
 */
class FactorySorter[CM[_], CF[+X[_]]] extends TopologicalSorter[CM[_], CF[CM]] {
  type HKClassManifest[CC2[_]] = ClassTag[CC2[_]]

  def add[T[A] <: CM[A] : HKClassManifest](companion: CF[T]): FactorySorter[CM, CF] = {
    companions += implicitly[HKClassManifest[T]].runtimeClass -> companion
    this
  }
}

class MapFactorySorter[CM[_, _], CF[+X[_, _]]] extends TopologicalSorter[CM[_, _], CF[CM]] {
  type HKClassManifest[CC2[_, _]] = ClassTag[CC2[_, _]]

  def add[T[K, V] <: CM[K, V] : HKClassManifest](companion: CF[T]): MapFactorySorter[CM, CF] = {
    companions += implicitly[HKClassManifest[T]].runtimeClass -> companion
    this
  }
}

trait TopologicalSorter[CC, CF] {
  protected[this] val companions = new ArrayBuffer[(Class[_], CF)]()

  def toList: List[(Class[_], CF)] = {
    val cs = companions.toArray
    val output = new ListBuffer[(Class[_], CF)]()

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
