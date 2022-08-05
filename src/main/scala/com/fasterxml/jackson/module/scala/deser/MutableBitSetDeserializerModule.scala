package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ArrayNode

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

private object MutableBitSetDeserializer extends StdDeserializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): mutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val array = arrayNode.elements().asScala.toArray.map(_.asInt())
    mutable.BitSet(array: _*)
  }
}

private[deser] object MutableBitSetDeserializerResolver extends Deserializers.Base {
  private val CLASS = classOf[mutable.BitSet]

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[mutable.BitSet] =
    if (CLASS isAssignableFrom javaType.getRawClass)
      MutableBitSetDeserializer
    else null
}
