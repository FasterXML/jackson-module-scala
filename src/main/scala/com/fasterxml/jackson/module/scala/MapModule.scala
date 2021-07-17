package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.deser.{SortedMapDeserializerModule, UnsortedMapDeserializerModule}
import com.fasterxml.jackson.module.scala.ser.MapSerializerModule

trait MapModule
  extends MapSerializerModule
    with UnsortedMapDeserializerModule
    with SortedMapDeserializerModule

class MapModuleInstance(override val config: ScalaModule.Config) extends MapModule

object MapModule extends MapModule
