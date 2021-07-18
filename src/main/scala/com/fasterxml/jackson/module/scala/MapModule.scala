package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{SortedMapDeserializerModule, UnsortedMapDeserializerModule}
import com.fasterxml.jackson.module.scala.ser.MapSerializerModule

trait MapModule
  extends MapSerializerModule
    with UnsortedMapDeserializerModule
    with SortedMapDeserializerModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    MapSerializerModule.initScalaModule(config)
    UnsortedMapDeserializerModule.initScalaModule(config)
    SortedMapDeserializerModule.initScalaModule(config)
  }
}


object MapModule extends MapModule
