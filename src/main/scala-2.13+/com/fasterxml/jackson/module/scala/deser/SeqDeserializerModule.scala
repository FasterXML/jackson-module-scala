package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection._

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = new FactorySorter[Collection, IterableFactory]()
      .add(IndexedSeq)
      .add(Iterable)
      .add(Seq)
      .add(LinearSeq)
      .add(immutable.Iterable)
      .add(immutable.IndexedSeq)
      .add(immutable.LazyList)
      .add(immutable.LinearSeq)
      .add(immutable.List)
      .add(immutable.Queue)
      .add(immutable.Stream)
      .add(immutable.Seq)
      .add(immutable.Vector)
      .add(mutable.ArrayBuffer)
      // .add(mutable.ArraySeq) TODO
      .add(mutable.ArrayDeque)
      .add(mutable.Buffer)
      .add(mutable.IndexedSeq)
      .add(mutable.Iterable)
      .add(mutable.ListBuffer)
      .add(mutable.Queue)
      .add(mutable.Seq)
      .add(mutable.Stack)
      .toList

    override def builderFor[A](cf: Factory, valueType: JavaType): mutable.Builder[A, Collection[A]] = cf.newBuilder[A]
  })
}
