package com.fasterxml.jackson.module.scala

import deser.TupleDeserializerModule
import ser.TupleSerializerModule

trait TupleModule extends TupleSerializerModule with TupleDeserializerModule {
  self: JacksonModule =>
}