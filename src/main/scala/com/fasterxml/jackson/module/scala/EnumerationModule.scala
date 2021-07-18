package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * Adds serialization and deserization support for Scala Enumerations.
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    EnumerationDeserializerModule.initScalaModule(config)
    EnumerationSerializerModule.initScalaModule(config)
  }
}

object EnumerationModule extends EnumerationModule
