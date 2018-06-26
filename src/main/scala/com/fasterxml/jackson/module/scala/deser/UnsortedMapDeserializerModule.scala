package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, MapDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.immutable.HashMap
import scala.collection.{GenMap, mutable}
import scala.language.existentials

private class MapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), GenMap[K,V]]) extends java.util.AbstractMap[K,V] {
  override def put(k: K, v: V): V = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[java.util.Map.Entry[K, V]] = throw new UnsupportedOperationException
}

private object UnsortedMapDeserializer {
  def builderFor(cls: Class[_]): mutable.Builder[(AnyRef,AnyRef), GenMap[AnyRef,AnyRef]] =
    if (classOf[HashMap[_,_]].isAssignableFrom(cls)) HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.HashMap[_,_]].isAssignableFrom(cls)) mutable.HashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.ListMap[_,_]].isAssignableFrom(cls)) mutable.ListMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.LinkedHashMap[_,_]].isAssignableFrom(cls)) mutable.LinkedHashMap.newBuilder[AnyRef,AnyRef] else
    if (classOf[mutable.Map[_,_]].isAssignableFrom(cls)) mutable.Map.newBuilder[AnyRef,AnyRef] else
    Map.newBuilder[AnyRef,AnyRef]
}

private class UnsortedMapInstantiator(config: DeserializationConfig, valueType: MapLikeType) extends StdValueInstantiator(config, valueType) {
  override def canCreateUsingDefault = true
  override def createUsingDefault(ctxt: DeserializationContext) =
    new MapBuilderWrapper[AnyRef,AnyRef](UnsortedMapDeserializer.builderFor(valueType.getRawClass))
}

private class UnsortedMapDeserializer(collectionType: MapLikeType, containerDeserializer: MapDeserializer)
  extends ContainerDeserializerBase[GenMap[_,_]](collectionType) with ContextualDeserializer {

  def this(collectionType: MapLikeType, valueInstantiator: ValueInstantiator, keyDeser: KeyDeserializer, valueDeser: JsonDeserializer[_], valueTypeDeser: TypeDeserializer) = {
    this(collectionType, new MapDeserializer(collectionType, valueInstantiator, keyDeser, valueDeser.asInstanceOf[JsonDeserializer[AnyRef]], valueTypeDeser))
  }

  override def getContentType: JavaType = containerDeserializer.getContentType
  override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[MapDeserializer]
    new UnsortedMapDeserializer(collectionType, newDelegate)
  }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): GenMap[_,_] = {
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: MapBuilderWrapper[_,_] => wrapper.builder.result()
    }
  }
}

private object UnsortedMapDeserializerResolver extends Deserializers.Base {

  private val MAP = classOf[collection.Map[_,_]]
  private val SORTED_MAP = classOf[collection.SortedMap[_,_]]

  override def findMapLikeDeserializer(theType: MapLikeType,
                                       config: DeserializationConfig,
                                       beanDesc: BeanDescription,
                                       keyDeserializer: KeyDeserializer,
                                       elementTypeDeserializer: TypeDeserializer,
                                       elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = theType.getRawClass

    if (!MAP.isAssignableFrom(rawClass)) null
    else if (SORTED_MAP.isAssignableFrom(rawClass)) null
    else {
      val instantiator = new UnsortedMapInstantiator(config, theType)
      new UnsortedMapDeserializer(theType, instantiator, keyDeserializer, elementDeserializer, elementTypeDeserializer)
    }
  }
}

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += { _.addDeserializers(UnsortedMapDeserializerResolver) }
}
