package com.fasterxml.jackson.module.scala

import deser.SeqDeserializerModule
import ser.SeqSerializerModule

trait IterableModule extends SeqSerializerModule with SeqDeserializerModule {
  self: JacksonModule =>
}