package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{SortedSetDeserializerModule, UnsortedSetDeserializerModule}

trait SetModule extends UnsortedSetDeserializerModule with SortedSetDeserializerModule

class SetModuleInstance(override val config: ScalaModule.Config) extends SetModule

object SetModule extends SetModule
