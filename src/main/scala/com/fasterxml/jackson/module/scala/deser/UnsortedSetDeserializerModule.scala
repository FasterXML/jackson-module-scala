package com.fasterxml.jackson.module.scala.deser

import java.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializerBase, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule
import com.fasterxml.jackson.module.scala.util.CompanionSorter

import scala.collection.{IterableFactory, immutable, mutable}

private class SetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.Set[E]]) extends util.AbstractCollection[E] {

  override def add(e: E): Boolean = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  override def iterator(): util.Iterator[E] = null
  override def size() = 0
}

private object UnsortedSetDeserializer {
  val COMPANIONS: Iterable[(Class[_], IterableFactory[collection.Set])] = new CompanionSorter[collection.Set]()
    .add(immutable.HashSet)
    .add(immutable.ListSet)
    .add(immutable.Set)
    .add(mutable.HashSet)
    .add(mutable.LinkedHashSet)
    .add(mutable.Set)
    .toList

  def companionFor(cls: Class[_]): IterableFactory[collection.Set] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse Set

  def builderFor[A](cls: Class[_]): mutable.Builder[A, collection.Set[A]] = companionFor(cls).newBuilder[A]
}

private class SetInstantiator(config: DeserializationConfig, valueType: JavaType)
  extends StdValueInstantiator(config, valueType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new SetBuilderWrapper[AnyRef](UnsortedSetDeserializer.builderFor[AnyRef](valueType.getRawClass))

}

private class UnsortedSetDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[collection.Set[_]](collectionType)
  with ContextualDeserializer {

  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty): UnsortedSetDeserializer = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new UnsortedSetDeserializer(collectionType, newDelegate)
  }

  override def getContentType: JavaType = containerDeserializer.getContentType

  override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): collection.Set[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: SetBuilderWrapper[_] => wrapper.builder.result()
    }
}

private object UnsortedSetDeserializerResolver extends Deserializers.Base {

  private final val SET = classOf[collection.Set[_]]
  private final val SORTED_SET = classOf[collection.SortedSet[_]]

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SET.isAssignableFrom(rawClass)) null
    else if (SORTED_SET.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SetInstantiator(config, collectionType)
      new UnsortedSetDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }
}

trait UnsortedSetDeserializerModule extends SetTypeModifierModule {
  this += (_ addDeserializers UnsortedSetDeserializerResolver)
}
