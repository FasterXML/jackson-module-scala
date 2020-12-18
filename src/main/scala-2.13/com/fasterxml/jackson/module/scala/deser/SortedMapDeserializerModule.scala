package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection._
import scala.language.existentials

trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[SortedMap, SortedMapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[SortedMap[_, _]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[SortedMap[_, _]], SortedMap),
      (classOf[immutable.SortedMap[_, _]], immutable.SortedMap),
      (classOf[immutable.TreeMap[_, _]], immutable.TreeMap),
      (classOf[mutable.SortedMap[_, _]], mutable.SortedMap),
      (classOf[mutable.TreeMap[_, _]], mutable.TreeMap)
    ))

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] =
      factory.newBuilder[K, V](OrderingLocator.locate[K](keyType))
  })
}
