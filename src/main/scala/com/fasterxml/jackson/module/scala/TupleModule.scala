package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.TupleDeserializerModule
import com.fasterxml.jackson.module.scala.ser.TupleSerializerModule

/**
 * Adds support for serializing and deserializing Scala Tuples.
 */
trait TupleModule extends TupleSerializerModule with TupleDeserializerModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    TupleSerializerModule.initScalaModule(config)
    TupleDeserializerModule.initScalaModule(config)
  }
}

object TupleModule extends TupleModule
