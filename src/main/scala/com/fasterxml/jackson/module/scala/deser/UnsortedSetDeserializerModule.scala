package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.{DeserializationConfig, JavaType}
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Set, IterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[Set[_]], Set),
      (classOf[immutable.HashSet[_]], immutable.HashSet),
      (classOf[immutable.ListSet[_]], immutable.ListSet),
      (classOf[immutable.Set[_]], immutable.Set),
      (classOf[mutable.HashSet[_]], mutable.HashSet),
      (classOf[mutable.LinkedHashSet[_]], mutable.LinkedHashSet),
      (classOf[mutable.Set[_]], mutable.Set)
    ))

    override def builderFor[A](cf: Factory, javaType: JavaType): Builder[A] = cf.newBuilder[A]

    override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
      // TODO add implementation
      ???
    }
  })
}
