package com.fasterxml.jackson.module.scala

import deser.OptionDeserializerModule
import ser.OptionSerializerModule

/**
 * Adds support for serializing and deserializing Scala Options.
 */
trait OptionModule extends OptionSerializerModule with OptionDeserializerModule {
}