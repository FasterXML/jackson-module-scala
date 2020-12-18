package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule
import com.fasterxml.jackson.module.scala.util.FactorySorter

import scala.collection._
import scala.reflect.ClassTag

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[IndexedSeq[_]], IndexedSeq),
      (classOf[Iterable[_]], Iterable),
      (classOf[Seq[_]], Seq),
      (classOf[immutable.Iterable[_]], immutable.Iterable),
      (classOf[immutable.IndexedSeq[_]], immutable.IndexedSeq),
      (classOf[immutable.List[_]], immutable.List),
      (classOf[immutable.Queue[_]], immutable.Queue),
      (classOf[immutable.Stream[_]], immutable.Stream),
      (classOf[immutable.Seq[_]], immutable.Seq),
      (classOf[immutable.Vector[_]], immutable.Vector),
      (classOf[mutable.ArrayBuffer[_]], mutable.ArrayBuffer),
      (classOf[mutable.ArraySeq[_]], mutable.ArraySeq),
      (classOf[mutable.Buffer[_]], mutable.Buffer),
      (classOf[mutable.IndexedSeq[_]], mutable.IndexedSeq),
      (classOf[mutable.Iterable[_]], mutable.Iterable),
      (classOf[mutable.LinearSeq[_]], mutable.LinearSeq),
      (classOf[mutable.ListBuffer[_]], mutable.ListBuffer),
      (classOf[mutable.MutableList[_]], mutable.MutableList),
      (classOf[mutable.Queue[_]], mutable.Queue),
      (classOf[mutable.ResizableArray[_]], mutable.ResizableArray),
      (classOf[mutable.Seq[_]], mutable.Seq),
      (classOf[mutable.Stack[_]], mutable.Stack)
    ))

    override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

    // UnrolledBuffer is in a class of its own pre 2.13...
    override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = {
      if (classOf[mutable.UnrolledBuffer[_]].isAssignableFrom(cls)) {
        mutable.UnrolledBuffer.newBuilder[A](ClassTag(valueType.getRawClass))
      } else {
        super.builderFor[A](cls, valueType)
      }
    }
  })
}
