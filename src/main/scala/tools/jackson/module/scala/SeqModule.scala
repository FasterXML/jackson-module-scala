package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.SeqDeserializerModule
import tools.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing and deserializing Scala sequences.
 */
trait SeqModule extends IterableSerializerModule with SeqDeserializerModule {
  override def getModuleName: String = "SeqModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    IterableSerializerModule.getInitializers(config) ++
      SeqDeserializerModule.getInitializers(config)
  }
}

object SeqModule extends SeqModule
