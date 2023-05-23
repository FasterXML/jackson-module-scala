package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.ser.IteratorSerializerModule

trait IteratorModule extends IteratorSerializerModule {
  override def getModuleName: String = "IteratorModule"
}
