package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext, JsonDeserializer, JsonNode, KeyDeserializer}
import com.fasterxml.jackson.databind.deser.std.{JsonNodeDeserializer, StdDeserializer}
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, IteratorModule}

import scala.collection.immutable
import scala.languageFeature.postfixOps

/**
 * Adds support for deserializing Scala [[scala.collection.immutable.IntMap]]s. Scala IntMaps can already be
 * serialized using [[IteratorModule]] or [[DefaultScalaModule]].
 *
 * @since 2.14.0
 */
private class ImmutableIntMapDeserializer2(elementDeserializer: JsonDeserializer[_])
    extends StdDeserializer[immutable.IntMap[_]](classOf[immutable.IntMap[_]]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): immutable.IntMap[_] = {
    val objectNodeDeserializer = JsonNodeDeserializer.getDeserializer(classOf[ObjectNode])
    val node = objectNodeDeserializer.deserialize(p, ctxt).asInstanceOf[ObjectNode]
    val iter = node.fields()
    var map = immutable.IntMap[String]()
    while (iter.hasNext) {
      val entry = iter.next()
      //entry.getValue
      //elementDeserializer.
      map += (entry.getKey.toInt, entry.getValue.asText())
    }
    map
  }
}

class ImmutableIntMapDeserializerResolver2 extends Deserializers.Base {
  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!classOf[immutable.IntMap[_]].isAssignableFrom(theType.getRawClass)) None.orNull
    else {
      new ImmutableIntMapDeserializer2(elementDeserializer)
    }
  }
}
