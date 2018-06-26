package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection._

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = {
      val fs = new FactorySorter[Collection, IterableFactory]()
        .add(IndexedSeq)
        .add(Iterable)
        .add(Seq)
        .add(immutable.Iterable)
        .add(immutable.IndexedSeq)
        .add(immutable.List)
        .add(immutable.Queue)
        .add(immutable.Stream)
        .add(immutable.Seq)
        .add(immutable.Vector)
        .add(mutable.ArrayBuffer)
        // .add(mutable.ArraySeq) TODO
        .add(mutable.Buffer)
        .add(mutable.IndexedSeq)
        .add(mutable.Iterable)
        .add(mutable.LinearSeq)
        .add(mutable.ListBuffer)
        .add(mutable.MutableList)
        .add(mutable.Queue)
        .add(mutable.ResizableArray)
        .add(mutable.Seq)
        .add(mutable.Stack)

      // This is annoying, but is easier to stay backwards compatible.
      {
        import scala.collection.immutable._
        fs.add(LazyList)
      }
      {
        import scala.collection.mutable._
        fs.add(ArrayDeque)
      }

      fs.toList
    }

    override def builderFor[A](cf: Factory, valueType: JavaType): mutable.Builder[A, Collection[A]] = cf.newBuilder[A]
  })
}
