package com.fasterxml.jackson.module.scala

import ser.{IterableSerializerModule}

trait IterableModule extends IterableSerializerModule {
  self: JacksonModule =>
}