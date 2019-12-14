package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import com.fasterxml.jackson.module.scala.util.MapFactorySorter

import scala.collection._
import scala.collection.generic.GenMapFactory
import scala.language.existentials

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[GenMap, GenMapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[GenMap[_, _]]

    override val factories: List[(Class[_], Factory)] = new MapFactorySorter[Collection, GenMapFactory]()
      .add(GenMap)
      .add(Map)
      .add(immutable.HashMap)
      .add(immutable.ListMap)
      .add(immutable.Map)
      .add(mutable.HashMap)
      .add(mutable.LinkedHashMap)
      .add(mutable.ListMap)
      .add(mutable.Map)
      // WeakHashMap is omitted due to the unlikely use case
      .toList

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] = factory.newBuilder[K, V]

    override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
      // TODO add implementation
      ???
    }
  })
}
