package com.fasterxml.jackson.module.scala

import deser.SeqDeserializerModule
import ser.IterableSerializerModule

/**
 * Adds support for serializing and deserializing Scala sequences.
 */
trait SeqModule extends IterableSerializerModule with SeqDeserializerModule {
}