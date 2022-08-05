package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.languageFeature.postfixOps

private object ImmutableBitSetDeserializer extends StdDeserializer[immutable.BitSet](classOf[immutable.BitSet]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): immutable.BitSet = {
    val arrayNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ArrayNode])
    val arrayNode = arrayNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ArrayNode]
    val array = arrayNode.elements().asScala.toArray.map(_.asInt())
    immutable.BitSet(array: _*)
  }
}

private object ImmutableBitSetDeserializerResolver extends Deserializers.Base {
  private val CLASS = classOf[immutable.BitSet]

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[immutable.BitSet] =
    if (CLASS isAssignableFrom javaType.getRawClass)
      ImmutableBitSetDeserializer
    else null
}

trait ImmutableBitSetDeserializerModule extends JacksonModule {
  this += { _ addDeserializers ImmutableBitSetDeserializerResolver }
}

object ImmutableBitSetDeserializerModule extends ImmutableBitSetDeserializerModule
