package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.EnumerationDeserializerModule
import tools.jackson.module.scala.ser.EnumerationSerializerModule

/**
 * Adds serialization and deserialization support for Scala Enumerations.
 */
trait EnumerationModule extends EnumerationSerializerModule with EnumerationDeserializerModule {
  override def getModuleName: String = "EnumerationModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EnumerationDeserializerModule.getInitializers(config) ++
      EnumerationSerializerModule.getInitializers(config)
  }
}

object EnumerationModule extends EnumerationModule
