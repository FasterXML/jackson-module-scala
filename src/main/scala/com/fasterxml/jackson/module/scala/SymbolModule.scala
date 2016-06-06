package com.fasterxml.jackson.module.scala

import deser.SymbolDeserializerModule
import ser.SymbolSerializerModule

/**
  * Adds support for serializing and deserializing Scala Symbols without the '.
  */
trait SymbolModule extends SymbolSerializerModule with SymbolDeserializerModule {
}