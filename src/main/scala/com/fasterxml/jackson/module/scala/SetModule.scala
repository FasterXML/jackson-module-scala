package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{SortedSetDeserializerModule, UnsortedSetDeserializerModule}

trait SetModule extends UnsortedSetDeserializerModule with SortedSetDeserializerModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    UnsortedSetDeserializerModule.initScalaModule(config)
    SortedSetDeserializerModule.initScalaModule(config)
  }
}

object SetModule extends SetModule
