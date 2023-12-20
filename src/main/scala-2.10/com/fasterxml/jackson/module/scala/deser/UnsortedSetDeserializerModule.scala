package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Set, IterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]

    override val factories: Iterable[(Class[_], Factory)] = new FactorySorter[Collection, IterableFactory]()
      .add(Set)
      .add(immutable.HashSet)
      .add(immutable.ListSet)
      .add(immutable.Set)
      .add(mutable.HashSet)
      .add(mutable.LinkedHashSet)
      .add(mutable.Set)
      .toList

    override def builderFor[A](cf: Factory, javaType: JavaType): Builder[A] = cf.newBuilder[A]
  })
}
