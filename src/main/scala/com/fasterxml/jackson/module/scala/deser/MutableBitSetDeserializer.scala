package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.mutable.BitSet]]s. Scala Bitsets can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 * <p>
 * <b>Do not enable this module unless you are sure that no input is accepted from untrusted sources.</b>
 * </p>
 * Scala BitSets use memory based on the highest int value stored. So a BitSet with just one big int will use a lot
 * more memory than a Scala BitSet with many small ints stored in it.
 *
 * @since 2.14.0
 */
object MutableBitSetDeserializer extends StdDeserializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): mutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val elements = arrayNode.elements().asScala.toSeq.map(_.asInt())
    mutable.BitSet(elements: _*)
  }
}
