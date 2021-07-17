package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.SeqDeserializerModule
import com.fasterxml.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing and deserializing Scala sequences.
 */
trait SeqModule extends IterableSerializerModule with SeqDeserializerModule

class SeqModuleInstance(override val config: ScalaModule.Config) extends SeqModule

object SeqModule extends SeqModule
