package com.fasterxml.jackson.module.scala.deser

import java.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializerBase, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.modifiers.SeqTypeModifierModule
import com.fasterxml.jackson.module.scala.util.CompanionSorter

import scala.collection.{IterableFactory, immutable, mutable}

private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: Iterable[E]]) extends util.AbstractCollection[E] {

  override def add(e: E): Boolean = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  override def iterator() = null
  override def size() = 0
}

private object SeqDeserializer {
  val COMPANIONS: Iterable[(Class[_], IterableFactory[Iterable])] = new CompanionSorter[collection.Iterable]()
    .add(immutable.IndexedSeq)
    .add(mutable.Buffer)
    .add(mutable.IndexedSeq)
    .add(mutable.ListBuffer)
    .add(mutable.Iterable)
    .add(mutable.Queue)
    .add(mutable.Seq)
    .add(immutable.Queue)
    .add(immutable.LazyList)
    .toList

  def companionFor(cls: Class[_]): IterableFactory[collection.Iterable] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse Iterable

  def builderFor[A](cls: Class[_]): mutable.Builder[A, Iterable[A]] = companionFor(cls).newBuilder[A]
}

private class SeqInstantiator(config: DeserializationConfig, valueType: JavaType)
  extends StdValueInstantiator(config, valueType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new BuilderWrapper[AnyRef](SeqDeserializer.builderFor[AnyRef](valueType.getRawClass))
}

private class SeqDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[Iterable[_]](collectionType)
  with ContextualDeserializer {

  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty): SeqDeserializer = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SeqDeserializer(collectionType, newDelegate)
  }

  override def getContentType: JavaType = containerDeserializer.getContentType

  override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Iterable[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: BuilderWrapper[_] => wrapper.builder.result()
    }
}

private object SeqDeserializerResolver extends Deserializers.Base {

  lazy final val SEQ = classOf[Iterable[_]]

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                     config: DeserializationConfig,
                     beanDesc: BeanDescription,
                     elementTypeDeserializer: TypeDeserializer,
                     elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SEQ.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SeqInstantiator(config, collectionType)
      new SeqDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

}

trait SeqDeserializerModule extends SeqTypeModifierModule {
  this += (_ addDeserializers SeqDeserializerResolver)
}
