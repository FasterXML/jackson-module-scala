package tools.jackson.module.scala

import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.deser.EitherDeserializerModule
import tools.jackson.module.scala.ser.EitherSerializerModule

trait EitherModule extends EitherDeserializerModule with EitherSerializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EitherDeserializerModule.getInitializers(config) ++
      EitherSerializerModule.getInitializers(config)
  }
}

object EitherModule extends EitherModule
