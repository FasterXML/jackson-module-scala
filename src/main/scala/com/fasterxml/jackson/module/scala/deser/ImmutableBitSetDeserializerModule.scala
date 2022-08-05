package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.node.ArrayNode

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

private[deser] object ImmutableBitSetDeserializer extends StdDeserializer[immutable.BitSet](classOf[immutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): immutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val elements = arrayNode.elements().asScala.toSeq.map(_.asInt())
    immutable.BitSet(elements: _*)
  }
}
