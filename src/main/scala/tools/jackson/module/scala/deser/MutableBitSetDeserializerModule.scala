package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.jackson.JsonNodeDeserializer
import tools.jackson.databind.node.ArrayNode

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

private[deser] object MutableBitSetDeserializer extends StdDeserializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): mutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val elements = arrayNode.elements().asScala.toSeq.map(_.asInt())
    mutable.BitSet(elements: _*)
  }
}
