package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifierModule

import scala.collection._
import scala.reflect.ClassTag

trait SeqDeserializerModule extends ScalaTypeModifierModule {
  this += (_ addDeserializers new GenericFactoryDeserializerResolver[Iterable, IterableFactory] {
    override val CLASS_DOMAIN: Class[Collection[_]] = classOf[Iterable[_]]

    override val factories: Iterable[(Class[_], Factory)] = sortFactories(Vector(
      (classOf[IndexedSeq[_]], IndexedSeq),
      (classOf[Iterable[_]], Iterable),
      (classOf[Seq[_]], Seq),
      (classOf[LinearSeq[_]], LinearSeq),
      (classOf[immutable.Iterable[_]], immutable.Iterable),
      (classOf[immutable.IndexedSeq[_]], immutable.IndexedSeq),
      (classOf[immutable.LazyList[_]], immutable.LazyList),
      (classOf[immutable.LinearSeq[_]], immutable.LinearSeq),
      (classOf[immutable.List[_]], immutable.List),
      (classOf[immutable.Queue[_]], immutable.Queue),
      (classOf[immutable.Stream[_]], immutable.Stream),
      (classOf[immutable.Seq[_]], immutable.Seq),
      (classOf[immutable.Vector[_]], immutable.Vector),
      (classOf[mutable.ArrayBuffer[_]], mutable.ArrayBuffer),
      (classOf[mutable.ArrayDeque[_]], mutable.ArrayDeque),
      (classOf[mutable.Buffer[_]], mutable.Buffer),
      (classOf[mutable.IndexedSeq[_]], mutable.IndexedSeq),
      (classOf[mutable.Iterable[_]], mutable.Iterable),
      (classOf[mutable.ListBuffer[_]], mutable.ListBuffer),
      (classOf[mutable.Queue[_]], mutable.Queue),
      (classOf[mutable.Seq[_]], mutable.Seq),
      (classOf[mutable.Stack[_]], mutable.Stack)
    ))

    override def builderFor[A](cf: Factory, valueType: JavaType): Builder[A] = cf.newBuilder[A]

    // A few types need class tags and therefore do not use IterableFactory.
    type TagFactory = ClassTagIterableFactory[Collection]

    val tagFactories: Iterable[(Class[_], TagFactory)] = Seq(
      (classOf[mutable.ArraySeq[_]], mutable.ArraySeq),
      (classOf[mutable.UnrolledBuffer[_]], mutable.UnrolledBuffer)
    )

    def builderFor[A](cf: TagFactory, valueType: JavaType): Builder[A] =
      cf.newBuilder[A](ClassTag(valueType.getRawClass))

    def tryTagFactory[A](cls: Class[_], valueType: JavaType): Option[Builder[A]] = tagFactories
      .find(_._1.isAssignableFrom(cls))
      .map(_._2)
      .map(builderFor[A](_, valueType))

    override def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = tryTagFactory[A](cls, valueType)
        .getOrElse(super.builderFor[A](cls, valueType))
  })
}
