package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import scala.collection.immutable
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.IntMap]]s. Scala IntMaps can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
object ImmutableIntMapDeserializer extends StdDeserializer[immutable.IntMap[_]](classOf[immutable.IntMap[_]]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): immutable.IntMap[_] = {
    val node: JsonNode = p.getCodec.readTree(p)
    val iter = node.fields()
    var map = immutable.IntMap[String]()
    while (iter.hasNext) {
      val entry = iter.next()
      map += (entry.getKey.toInt, entry.getValue.asText())
    }
    map
  }
}
