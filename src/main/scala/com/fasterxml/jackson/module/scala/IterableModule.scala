package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing Scala Iterables.
 */
trait IterableModule extends IterableSerializerModule {
  override def getModuleName: String = "IterableModule"
}
