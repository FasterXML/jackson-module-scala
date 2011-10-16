package com.fasterxml.jackson.module.scala

import deser.TupleDeserializerModule
import ser.TupleSerializerModule

/**
 * Adds support for serializing and deserializing Scala Tuples.
 */
trait TupleModule extends TupleSerializerModule with TupleDeserializerModule {
}