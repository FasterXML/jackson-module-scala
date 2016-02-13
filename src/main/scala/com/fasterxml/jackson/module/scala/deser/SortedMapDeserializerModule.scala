package com.fasterxml.jackson.module.scala.deser

import java.util.AbstractMap
import java.util.Map.Entry

import scala.collection.{mutable, SortedMap}
import scala.collection.immutable.TreeMap

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.{MapDeserializer, ContainerDeserializerBase}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import scala.language.existentials

private class SortedMapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), SortedMap[K,V]]) extends AbstractMap[K,V] {
  override def put(k: K, v: V) = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[Entry[K, V]] = throw new UnsupportedOperationException
}

private object SortedMapDeserializer {
  def orderingFor = OrderingLocator.locate _

  def builderFor(cls: Class[_], keyCls: JavaType): mutable.Builder[(AnyRef,AnyRef), SortedMap[AnyRef,AnyRef]] =
    if (classOf[TreeMap[_,_]].isAssignableFrom(cls)) TreeMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls)) else
    SortedMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls))
}

private class SortedMapDeserializer(
    collectionType: MapLikeType,
    config: DeserializationConfig,
    keyDeser: KeyDeserializer,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)
  extends ContainerDeserializerBase[SortedMap[_,_]](collectionType)
  with ContextualDeserializer {
  
  private val javaContainerType =
    config.getTypeFactory.constructMapLikeType(classOf[MapBuilderWrapper[_,_]], collectionType.getKeyType, collectionType.getContentType)

  private val instantiator =
    new ValueInstantiator {
      def getValueTypeDesc = collectionType.getRawClass.getCanonicalName
      override def canCreateUsingDefault = true
      override def createUsingDefault(ctx: DeserializationContext) =
        new SortedMapBuilderWrapper[AnyRef,AnyRef](SortedMapDeserializer.builderFor(collectionType.getRawClass, collectionType.getKeyType))
    }

  private val containerDeserializer =
    new MapDeserializer(javaContainerType,instantiator,keyDeser,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) =
    if (keyDeser != null && valueDeser != null) this
    else {
      val newKeyDeser = Option(keyDeser).getOrElse(ctxt.findKeyDeserializer(collectionType.getKeyType, property))
      val newValDeser = Option(valueDeser).getOrElse(ctxt.findContextualValueDeserializer(collectionType.getContentType, property))
      new SortedMapDeserializer(collectionType, config, newKeyDeser, newValDeser, valueTypeDeser)
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
    else new SortedMapDeserializer(theType,config,keyDeserializer,elementDeserializer,elementTypeDeserializer)
}

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers SortedMapDeserializerResolver)
}