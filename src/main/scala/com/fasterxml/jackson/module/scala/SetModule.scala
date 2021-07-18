package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.{SortedSetDeserializerModule, UnsortedSetDeserializerModule}

trait SetModule extends UnsortedSetDeserializerModule with SortedSetDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    UnsortedSetDeserializerModule.getInitializers(config) ++
      SortedSetDeserializerModule.getInitializers(config)
  }
}

object SetModule extends SetModule
