package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.module.scala.ser.MapSerializerModule
import com.fasterxml.jackson.module.scala.deser.{UnsortedMapDeserializerModule, SortedMapDeserializerModule}

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait MapModule extends MapSerializerModule with SortedMapDeserializerModule with UnsortedMapDeserializerModule {

}