package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.SeqDeserializerModule
import com.fasterxml.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing and deserializing Scala sequences.
 */
trait SeqModule extends IterableSerializerModule with SeqDeserializerModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    IterableSerializerModule.initScalaModule(config)
    SeqDeserializerModule.initScalaModule(config)
  }
}

object SeqModule extends SeqModule
