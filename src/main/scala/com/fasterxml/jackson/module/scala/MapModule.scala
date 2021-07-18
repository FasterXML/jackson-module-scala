package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.module.scala.deser.{SortedMapDeserializerModule, UnsortedMapDeserializerModule}
import com.fasterxml.jackson.module.scala.ser.MapSerializerModule

trait MapModule
  extends MapSerializerModule
    with UnsortedMapDeserializerModule
    with SortedMapDeserializerModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    MapSerializerModule.getInitializers(config) ++
      UnsortedMapDeserializerModule.getInitializers(config) ++
      SortedMapDeserializerModule.getInitializers(config)
  }
}


object MapModule extends MapModule
