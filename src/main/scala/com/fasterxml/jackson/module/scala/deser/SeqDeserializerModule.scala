package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.SeqTypeModifierModule

import com.fasterxml.jackson.core.JsonParser

import com.fasterxml.jackson.databind.{BeanDescription, BeanProperty, JsonDeserializer, JavaType, DeserializationContext, DeserializationConfig}
import com.fasterxml.jackson.databind.deser.std.{ContainerDeserializerBase, CollectionDeserializer, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{Deserializers, ValueInstantiator, ContextualDeserializer}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.`type`.CollectionLikeType

import collection.generic.GenericCompanion
import collection.immutable.Queue

import java.util.AbstractCollection
import scala.collection.mutable
import com.fasterxml.jackson.module.scala.util.CompanionSorter

private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: Iterable[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object SeqDeserializer {
  val COMPANIONS = new CompanionSorter[collection.Seq]()
    .add(IndexedSeq)
    .add(mutable.ArraySeq)
    .add(mutable.Buffer)
    .add(mutable.IndexedSeq)
    .add(mutable.LinearSeq)
    .add(mutable.ListBuffer)
    .add(mutable.MutableList)
    .add(mutable.Queue)
    .add(mutable.ResizableArray)
    .add(Queue)
    .add(Stream)
    .toList

  def companionFor(cls: Class[_]): GenericCompanion[collection.Iterable] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse Iterable

  def builderFor[A](cls: Class[_]): mutable.Builder[A,Iterable[A]] = companionFor(cls).newBuilder[A]
}

private class SeqInstantiator(config: DeserializationConfig, valueType: Class[_])
  extends StdValueInstantiator(config, valueType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new BuilderWrapper[AnyRef](SeqDeserializer.builderFor[AnyRef](valueType))  
}

private class SeqDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[Iterable[_]](collectionType)
  with ContextualDeserializer {

  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SeqDeserializer(collectionType, newDelegate)
  }

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

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
      val instantiator = new SeqInstantiator(config, rawClass)
      new SeqDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

}

trait SeqDeserializerModule extends SeqTypeModifierModule {
  this += (_ addDeserializers SeqDeserializerResolver)
}