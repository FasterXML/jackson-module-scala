package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.SymbolDeserializerModule
import com.fasterxml.jackson.module.scala.ser.SymbolSerializerModule

/**
 * Adds support for serializing and deserializing Scala Symbols without the '.
 */
trait SymbolModule extends SymbolSerializerModule with SymbolDeserializerModule

object SymbolModule extends SymbolModule

class SymbolModuleInstance(override val builder: ScalaModule.ReadOnlyBuilder) extends SymbolModule

