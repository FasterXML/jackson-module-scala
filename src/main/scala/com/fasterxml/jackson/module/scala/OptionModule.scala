package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.OptionDeserializerModule
import com.fasterxml.jackson.module.scala.ser.OptionSerializerModule

/**
 * Adds support for serializing and deserializing Scala Options.
 */
trait OptionModule extends OptionSerializerModule with OptionDeserializerModule {
  override def getModuleName: String = "OptionModule"
}
