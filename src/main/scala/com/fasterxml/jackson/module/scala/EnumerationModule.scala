package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * Adds serialization and deserization support for Scala Enumerations.
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule

class EnumerationModuleInstance(override val config: ScalaModule.Config) extends EnumerationModule

object EnumerationModule extends EnumerationModule
