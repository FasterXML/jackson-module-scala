package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection.{immutable, mutable}

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[collection.Set, collection.IterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Collection[_]]

    override val factories: Iterable[(Class[_], Factory)] = new FactorySorter[Collection, collection.IterableFactory]()
      .add(immutable.HashSet)
      .add(immutable.ListSet)
      .add(immutable.Set)
      .add(mutable.HashSet)
      .add(mutable.LinkedHashSet)
      .add(mutable.Set)
      .toList

    override def builderFor[A](cf: Factory, javaType: JavaType): mutable.Builder[A, Collection[A]] = cf.newBuilder[A]
  })
}
