package com.fasterxml.jackson.module.scala.deser

import java.util.AbstractMap
import java.util.Map.Entry

import scala.collection.{mutable, SortedMap, TreeMap};

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer};
import com.fasterxml.jackson.databind.deser.{Deserializers, ValueInstantiator};
import com.fasterxml.jackson.databind.deser.std,{MapDeserializer, ContainerDeserializer};

import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

private class SortedMapBuilderWrapper[K,V](val builder: mutable.Builder[(K,V), SortedMap[K,V]]) extends AbstractMap[K,V] {
  override def put(k: K, v: V) = { builder += ((k,v)); v }

  // Isn't used by the deserializer
  def entrySet(): java.util.Set[Entry[K, V]] = throw new UnsupportedOperationException
}

private object SortedMapDeserializer {
  def orderingFor(cls: Class[_]): Ordering[AnyRef] =
    (if (classOf[String].isAssignableFrom(cls)) Ordering.String else
    throw new IllegalArgumentException("Unsupported key type: " + cls.getCanonicalName)).asInstanceOf[Ordering[AnyRef]]

  def builderFor(cls: Class[_], keyCls: Class[_]): mutable.Builder[(AnyRef,AnyRef), SortedMap[AnyRef,AnyRef]] =
    if (classOf[TreeMap[_,_]].isAssignableFrom(cls)) TreeMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls)) else
    SortedMap.newBuilder[AnyRef,AnyRef](orderingFor(keyCls))
}

private class SortedMapDeserializer(
    collectionType: MapLikeType,
    config: DeserializationConfig,
    keyDeser: KeyDeserializer,
    valueDeser: JsonDeserializer[_],
    valueTypeDeser: TypeDeserializer)
  extends ContainerDeserializer[SortedMap[_,_]](classOf[SortedMapDeserializer]) {
  private val javaContainerType = config.constructType(classOf[MapBuilderWrapper[AnyRef,AnyRef]])

  private val instantiator =
    new ValueInstantiator {
      def getValueTypeDesc = collectionType.getRawClass.getCanonicalName
      override def canCreateUsingDefault = true
      override def createUsingDefault =
        new SortedMapBuilderWrapper[AnyRef,AnyRef](SortedMapDeserializer.builderFor(collectionType.getRawClass, collectionType.containedType(0).getRawClass))
    }

  private val containerDeserializer =
    new MapDeserializer(javaContainerType,instantiator,keyDeser,valueDeser.asInstanceOf[JsonDeserializer[AnyRef]],valueTypeDeser)

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  def deserialize(jp: JsonParser, ctxt: DeserializationContext): SortedMap[_,_] = {
    containerDeserializer.deserialize(jp,ctxt) match {
      case wrapper: SortedMapBuilderWrapper[_,_] => wrapper.builder.result()
    }
  }
}

private object SortedMapDeserializerResolver extends Deserializers.Base {
  override def findMapLikeDeserializer(theType: MapLikeType,
                              config: DeserializationConfig,
                              provider: DeserializerProvider,
                              beanDesc: BeanDescription,
                              property: BeanProperty,
                              keyDeserializer: KeyDeserializer,
                              elementTypeDeserializer: TypeDeserializer,
                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = theType.getRawClass
    if (classOf[collection.SortedMap[_,_]].isAssignableFrom(rawClass)) {
      val keyType = theType.containedType(0)
      val valueType = theType.containedType(1)
      val resolvedKeyDeser =
        Option(keyDeserializer).getOrElse(provider.findKeyDeserializer(config,keyType,property))
      val resolvedValueDeser =
        Option(elementDeserializer).getOrElse(provider.findValueDeserializer(config,valueType,property))
      new SortedMapDeserializer(theType,config,resolvedKeyDeser,resolvedValueDeser,elementTypeDeserializer)
    } else null
  }

}

/**
 * @author Christopher Currie <christopher@currie.com>
 */
trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += { _.addDeserializers(SortedMapDeserializerResolver) }
}