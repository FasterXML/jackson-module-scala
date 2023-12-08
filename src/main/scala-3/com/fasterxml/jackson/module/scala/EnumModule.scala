package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.EnumDeserializerModule
import com.fasterxml.jackson.module.scala.ser.EnumSerializerModule

trait EnumModule extends EnumSerializerModule with EnumDeserializerModule {
  override def getModuleName: String = "EnumModule"
}

object EnumModule extends EnumModule
