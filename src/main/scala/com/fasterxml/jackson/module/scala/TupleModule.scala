package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.TupleDeserializerModule
import com.fasterxml.jackson.module.scala.ser.TupleSerializerModule

/**
 * Adds support for serializing and deserializing Scala Tuples.
 */
trait TupleModule extends TupleSerializerModule with TupleDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    TupleSerializerModule.getInitializers(config) ++
      TupleDeserializerModule.getInitializers(config)
  }
}

object TupleModule extends TupleModule
