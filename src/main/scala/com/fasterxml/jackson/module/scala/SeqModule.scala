package com.fasterxml.jackson.module.scala

import deser.SeqDeserializerModule
import ser.SeqSerializerModule

trait SeqModule extends SeqSerializerModule with SeqDeserializerModule {
  self: JacksonModule =>
}