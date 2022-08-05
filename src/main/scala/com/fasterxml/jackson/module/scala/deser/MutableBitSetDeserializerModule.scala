package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind.node.ArrayNode

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

private[deser] object MutableBitSetDeserializer extends StdDeserializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): mutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val array = arrayNode.elements().asScala.toArray.map(_.asInt())
    mutable.BitSet(array: _*)
  }
}
