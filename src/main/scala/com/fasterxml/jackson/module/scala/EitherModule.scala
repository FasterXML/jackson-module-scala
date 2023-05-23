package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EitherDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EitherSerializerModule

trait EitherModule extends EitherDeserializerModule with EitherSerializerModule {
  override def getModuleName: String = "EitherModule"
}