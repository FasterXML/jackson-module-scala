package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection.{immutable, mutable}

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[collection.Iterable, collection.IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Collection[_]]

    override val factories: Iterable[(Class[_], Factory)] = new FactorySorter[Collection, collection.IterableFactory]()
      .add(Iterable)
      .add(immutable.Iterable)
      .add(immutable.IndexedSeq)
      .add(immutable.LazyList)
      .add(immutable.List)
      .add(immutable.Queue)
      .add(immutable.Stream)
      .add(immutable.Seq)
      .add(mutable.ArrayBuffer)
      .add(mutable.Buffer)
      .add(mutable.IndexedSeq)
      .add(mutable.ListBuffer)
      .add(mutable.Iterable)
      .add(mutable.Queue)
      .add(mutable.Seq)
      .toList

    override def builderFor[A](cf: Factory): mutable.Builder[A, Collection[A]] = cf.newBuilder[A]
  })
}
