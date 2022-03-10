package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.EnumerationDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * Adds serialization and deserialization support for Scala Enumerations.
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EnumerationDeserializerModule.getInitializers(config) ++
      EnumerationSerializerModule.getInitializers(config)
  }
}

object EnumerationModule extends EnumerationModule
