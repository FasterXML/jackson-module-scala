package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection._
import scala.language.existentials

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[Map, MapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[Map[_, _]]

    // OpenHashMap is omitted due to deprecation
    // WeakHashMap is omitted due to the unlikely use case
    override val factories: scala.Seq[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[Map[_, _]], Map),
      (classOf[immutable.HashMap[_, _]], immutable.HashMap),
      (classOf[immutable.ListMap[_, _]], immutable.ListMap),
      (classOf[immutable.Map[_, _]], immutable.Map),
      (classOf[mutable.HashMap[_, _]], mutable.HashMap),
      (classOf[mutable.LinkedHashMap[_, _]], mutable.LinkedHashMap),
      (classOf[mutable.ListMap[_, _]], mutable.ListMap),
      (classOf[mutable.Map[_, _]], mutable.Map),
      (classOf[concurrent.TrieMap[_, _]], concurrent.TrieMap)
    ))

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] = factory.newBuilder[K, V]
  })
}
