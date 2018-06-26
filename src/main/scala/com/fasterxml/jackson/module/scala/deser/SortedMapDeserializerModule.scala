package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, MapDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.immutable.TreeMap
import scala.collection.{SortedMap, mutable}
import scala.language.existentials

private class SortedMapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), SortedMap[K,V]]) extends java.util.AbstractMap[K,V] {
  override def put(k: K, v: V): V = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[java.util.Map.Entry[K, V]] = throw new UnsupportedOperationException
}

private object SortedMapDeserializer {
  private def orderingFor = OrderingLocator.locate _

  def builderFor(cls: Class[_], keyCls: JavaType): mutable.Builder[(AnyRef,AnyRef), SortedMap[AnyRef,AnyRef]] =
    if (classOf[TreeMap[_,_]].isAssignableFrom(cls)) TreeMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls)) else
    SortedMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls))
}

private class SortedMapInstantiator(config: DeserializationConfig, mapType: MapLikeType) extends StdValueInstantiator(config, mapType) {
  override def canCreateUsingDefault = true
  override def createUsingDefault(ctxt: DeserializationContext) =
    new SortedMapBuilderWrapper[AnyRef,AnyRef](SortedMapDeserializer.builderFor(mapType.getRawClass, mapType.getKeyType))
}

private class SortedMapDeserializer(mapType: MapLikeType, containerDeserializer: MapDeserializer)
  extends ContainerDeserializerBase[SortedMap[_,_]](mapType) with ContextualDeserializer {

  def this(mapType: MapLikeType, valueInstantiator: ValueInstantiator, keyDeser: KeyDeserializer, valueDeser: JsonDeserializer[_], valueTypeDeser: TypeDeserializer) = {
    this(mapType, new MapDeserializer(mapType, valueInstantiator, keyDeser, valueDeser.asInstanceOf[JsonDeserializer[AnyRef]], valueTypeDeser))
  }

  override def getContentType: JavaType = containerDeserializer.getContentType
  override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
    new SortedMapDeserializer(mapType, newDelegate)
  }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SortedMap[_,_] = {
    containerDeserializer.deserialize(jp,ctxt) match {
      case wrapper: SortedMapBuilderWrapper[_,_] => wrapper.builder.result()
    }
  }
}

private object SortedMapDeserializerResolver extends Deserializers.Base {

  private val SORTED_MAP = classOf[collection.SortedMap[_,_]]

  override def findMapLikeDeserializer(theType: MapLikeType,
                              config: DeserializationConfig,
                              beanDesc: BeanDescription,
                              keyDeserializer: KeyDeserializer,
                              elementTypeDeserializer: TypeDeserializer,
                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] =
    if (!SORTED_MAP.isAssignableFrom(theType.getRawClass)) null
    else {
      val instantiator = new SortedMapInstantiator(config, theType)
      new SortedMapDeserializer(theType, instantiator, keyDeserializer, elementDeserializer, elementTypeDeserializer)
    }
}

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers SortedMapDeserializerResolver)
}
