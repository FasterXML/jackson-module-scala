package tools.jackson.module.scala.deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.{DeserializationContext, DeserializationFeature}
import tools.jackson.databind.deser.jackson.JsonNodeDeserializer
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.scala.ScalaModule

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.BitSet]]s. Scala Bitsets can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 * <p>
 * <b>Do not enable this module unless you are sure that no input is accepted from untrusted sources.</b>
 * </p>
 * Scala BitSets use memory based on the highest int value stored. So a BitSet with just one big int will use a lot
 * more memory than a Scala BitSet with many small ints stored in it.
 *
 * @since 2.14.0
 */
class ImmutableBitSetDeserializer(config: ScalaModule.Config)
    extends StdDeserializer[immutable.BitSet](classOf[immutable.BitSet]) {

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): immutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val elements = arrayNode.values().asScala.toSeq.map(_.asInt())
    immutable.BitSet(elements: _*)
  }

  override def getEmptyValue(ctxt: DeserializationContext): immutable.BitSet =
    immutable.BitSet.empty

  override def getNullValue(ctxt: DeserializationContext): immutable.BitSet = {
    if (!config.shouldDeserializeNullCollectionsAsEmpty() ||
        ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES))
      super.getNullValue(ctxt).asInstanceOf[immutable.BitSet]
    else
      getEmptyValue(ctxt)
  }
}
