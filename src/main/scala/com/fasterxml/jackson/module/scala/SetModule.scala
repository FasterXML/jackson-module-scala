package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{SortedSetDeserializerModule, UnsortedSetDeserializerModule}

trait SetModule extends UnsortedSetDeserializerModule with SortedSetDeserializerModule {
  override def getModuleName: String = "SetModule"
}
