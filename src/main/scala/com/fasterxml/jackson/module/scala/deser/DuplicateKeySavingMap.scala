package com.fasterxml.jackson.module.scala.deser

import scala.collection.mutable

private[deser] class DuplicateKeySavingMap[K] extends mutable.AbstractMap[K, Any] {
  private val map = mutable.Map[K, Any]()

  override def addOne(elem: (K, Any)): DuplicateKeySavingMap.this.type = {
    map.get(elem._1) match {
      case Some(v) => v match {
        case s: Seq[(Any, Any)] => map.put(elem._1, s :+ elem._2)
        case x => map.put(elem._1, Seq(x, elem._2))
      }
      case _ => map.addOne(elem)
    }
    this
  }

  override def get(key: K): Option[Any] = map.get(key)

  override def subtractOne(elem: K): DuplicateKeySavingMap.this.type = {
    map.subtractOne(elem)
    this
  }

  override def iterator: Iterator[(K, Any)] = map.iterator

  def toMap: Map[K, Any] = map.toMap

  def toMutableMap: mutable.Map[K, Any] = map
}
