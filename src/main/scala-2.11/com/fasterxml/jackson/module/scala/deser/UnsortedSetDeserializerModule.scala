package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._

trait UnsortedSetDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Set, IterableFactory] {

    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Set[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[Set[_]], Set.asInstanceOf[Factory]),
      (classOf[immutable.HashSet[_]], immutable.HashSet.asInstanceOf[Factory]),
      (classOf[immutable.ListSet[_]], immutable.ListSet.asInstanceOf[Factory]),
      (classOf[immutable.Set[_]], immutable.Set.asInstanceOf[Factory]),
      (classOf[mutable.HashSet[_]], mutable.HashSet.asInstanceOf[Factory]),
      (classOf[mutable.LinkedHashSet[_]], mutable.LinkedHashSet.asInstanceOf[Factory]),
      (classOf[mutable.Set[_]], mutable.Set.asInstanceOf[Factory])
    ))

    override def builderFor[A](cf: Factory, javaType: JavaType): Builder[A] = cf.newBuilder[A]
  })
}
