package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.EitherDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EitherSerializerModule

trait EitherModule extends EitherDeserializerModule with EitherSerializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    EitherDeserializerModule.getInitializers(config) ++
      EitherSerializerModule.getInitializers(config)
  }
}

object EitherModule extends EitherModule
