package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._
import scala.reflect.ClassTag

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[IndexedSeq[_]], IndexedSeq.asInstanceOf[Factory]),
      (classOf[Iterable[_]], Iterable.asInstanceOf[Factory]),
      (classOf[Seq[_]], Seq.asInstanceOf[Factory]),
      (classOf[immutable.Iterable[_]], immutable.Iterable.asInstanceOf[Factory]),
      (classOf[immutable.IndexedSeq[_]], immutable.IndexedSeq.asInstanceOf[Factory]),
      (classOf[immutable.List[_]], immutable.List.asInstanceOf[Factory]),
      (classOf[immutable.Queue[_]], immutable.Queue.asInstanceOf[Factory]),
      (classOf[immutable.Stream[_]], immutable.Stream.asInstanceOf[Factory]),
      (classOf[immutable.Seq[_]], immutable.Seq.asInstanceOf[Factory]),
      (classOf[immutable.Vector[_]], immutable.Vector.asInstanceOf[Factory]),
      (classOf[mutable.ArrayBuffer[_]], mutable.ArrayBuffer.asInstanceOf[Factory]),
      (classOf[mutable.ArraySeq[_]], mutable.ArraySeq.asInstanceOf[Factory]),
      (classOf[mutable.Buffer[_]], mutable.Buffer.asInstanceOf[Factory]),
      (classOf[mutable.IndexedSeq[_]], mutable.IndexedSeq.asInstanceOf[Factory]),
      (classOf[mutable.Iterable[_]], mutable.Iterable.asInstanceOf[Factory]),
      (classOf[mutable.LinearSeq[_]], mutable.LinearSeq.asInstanceOf[Factory]),
      (classOf[mutable.ListBuffer[_]], mutable.ListBuffer.asInstanceOf[Factory]),
      (classOf[mutable.MutableList[_]], mutable.MutableList.asInstanceOf[Factory]),
      (classOf[mutable.Queue[_]], mutable.Queue.asInstanceOf[Factory]),
      (classOf[mutable.ResizableArray[_]], mutable.ResizableArray.asInstanceOf[Factory]),
      (classOf[mutable.Seq[_]], mutable.Seq.asInstanceOf[Factory]),
      (classOf[mutable.Stack[_]], mutable.Stack.asInstanceOf[Factory])
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
