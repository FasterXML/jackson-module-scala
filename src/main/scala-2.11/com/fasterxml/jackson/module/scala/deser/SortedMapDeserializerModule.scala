package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import com.fasterxml.jackson.module.scala.util.MapFactorySorter

import scala.collection._
import scala.collection.generic.SortedMapFactory
import scala.language.existentials

trait SortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[SortedMap, SortedMapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[SortedMap[_, _]]

    override val factories: List[(Class[_], Factory)] = new MapFactorySorter[Collection, SortedMapFactory]()
      .add(SortedMap)
      .add(immutable.SortedMap)
      .add(immutable.TreeMap)
      .toList

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] =
      factory.newBuilder[K, V](OrderingLocator.locate[K](keyType))
  })
}
