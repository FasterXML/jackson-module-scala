package com.fasterxml.jackson.module.scala.introspect

private[introspect] object LazyListSupport {
  type LazyListType[T] = Stream[T]

  def empty[T]: LazyListType[T] = Stream.empty[T]

  def fromArray[T](array: Array[T]): LazyListType[T] = array.toStream
}
