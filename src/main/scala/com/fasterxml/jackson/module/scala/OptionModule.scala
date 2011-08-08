package com.fasterxml.jackson.module.scala

import deser.OptionDeserializerModule
import ser.OptionSerializerModule

trait OptionModule extends OptionSerializerModule with OptionDeserializerModule {
  self: JacksonModule =>
}