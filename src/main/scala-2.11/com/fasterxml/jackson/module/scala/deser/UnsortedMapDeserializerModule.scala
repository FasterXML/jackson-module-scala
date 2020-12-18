package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection._
import scala.collection.generic.GenMapFactory
import scala.language.existentials

trait UnsortedMapDeserializerModule extends MapTypeModifierModule {
  this += (_ addDeserializers new GenericMapFactoryDeserializerResolver[GenMap, GenMapFactory] {

    override val CLASS_DOMAIN: Class[Collection[_, _]] = classOf[GenMap[_, _]]

    // WeakHashMap is omitted due to the unlikely use case
    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[GenMap[_, _]], GenMap.asInstanceOf[Factory]),
      (classOf[Map[_, _]], Map.asInstanceOf[Factory]),
      (classOf[immutable.HashMap[_, _]], immutable.HashMap.asInstanceOf[Factory]),
      (classOf[immutable.ListMap[_, _]], immutable.ListMap.asInstanceOf[Factory]),
      (classOf[immutable.Map[_, _]], immutable.Map.asInstanceOf[Factory]),
      (classOf[mutable.HashMap[_, _]], mutable.HashMap.asInstanceOf[Factory]),
      (classOf[mutable.LinkedHashMap[_, _]], mutable.LinkedHashMap.asInstanceOf[Factory]),
      (classOf[mutable.ListMap[_, _]], mutable.ListMap.asInstanceOf[Factory]),
      (classOf[mutable.Map[_, _]], mutable.Map.asInstanceOf[Factory]),
      (classOf[concurrent.TrieMap[_, _]], concurrent.TrieMap.asInstanceOf[Factory])
    ))

    override def builderFor[K, V](factory: Factory, keyType: JavaType, valueType: JavaType): Builder[K, V] = factory.newBuilder[K, V]
  })
}
