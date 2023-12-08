package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.EnumDeserializerModule
import tools.jackson.module.scala.ser.EnumSerializerModule

trait EnumModule extends EnumSerializerModule with EnumDeserializerModule {
  override def getModuleName: String = "EnumModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EnumSerializerModule.getInitializers(config) ++
      EnumDeserializerModule.getInitializers(config)
  }
}

object EnumModule extends EnumModule
