package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.SymbolDeserializerModule
import tools.jackson.module.scala.ser.SymbolSerializerModule

/**
 * Adds support for serializing and deserializing Scala Symbols without the '.
 */
trait SymbolModule extends SymbolSerializerModule with SymbolDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    SymbolSerializerModule.getInitializers(config) ++
      SymbolDeserializerModule.getInitializers(config)
  }
}

object SymbolModule extends SymbolModule
