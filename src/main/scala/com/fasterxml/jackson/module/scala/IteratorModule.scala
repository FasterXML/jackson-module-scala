package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.ser.IteratorSerializerModule

trait IteratorModule extends IteratorSerializerModule

class IteratorModuleInstance(override val config: ScalaModule.Config) extends IteratorModule

object IteratorModule extends IteratorModule
