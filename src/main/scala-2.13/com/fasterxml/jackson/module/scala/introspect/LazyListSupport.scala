package com.fasterxml.jackson.module.scala.introspect

private[introspect] object LazyListSupport {
  type LazyListType[T] = LazyList[T]

  def empty[T]: LazyListType[T] = LazyList.empty[T]

  def fromArray[T](array: Array[T]): LazyListType[T] = array.to(LazyList)
}
