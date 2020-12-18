package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.{DeserializationConfig, JavaType}
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait SortedSetDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[SortedSet, SortedIterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[SortedSet[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[SortedSet[_]], SortedSet),
      (classOf[immutable.TreeSet[_]], immutable.TreeSet),
      (classOf[immutable.SortedSet[_]], immutable.SortedSet),
      (classOf[mutable.TreeSet[_]], mutable.TreeSet),
      (classOf[mutable.SortedSet[_]], mutable.SortedSet)
    ))

    override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] =
      cf.newBuilder[A](OrderingLocator.locate(valueType).asInstanceOf[Ordering[A]])

    override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
      // TODO add implementation
      ???
    }
  })
}
