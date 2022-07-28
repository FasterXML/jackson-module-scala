package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.OptionDeserializerModule
import tools.jackson.module.scala.ser.OptionSerializerModule

/**
 * Adds support for serializing and deserializing Scala Options.
 */
trait OptionModule extends OptionSerializerModule with OptionDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    OptionSerializerModule.getInitializers(config) ++
      OptionDeserializerModule.getInitializers(config)
  }
}

object OptionModule extends OptionModule
