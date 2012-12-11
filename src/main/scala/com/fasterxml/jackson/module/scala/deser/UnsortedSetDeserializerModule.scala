package com.fasterxml.jackson.module.scala.deser

import scala.collection.generic.GenericCompanion
import java.util.AbstractCollection
import scala.collection.{immutable, mutable}
import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.{StdValueInstantiator, CollectionDeserializer, ContainerDeserializerBase}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.{BeanProperty, JavaType, BeanDescription, DeserializationContext, JsonDeserializer, DeserializationConfig}
import com.fasterxml.jackson.module.scala.util.CompanionSorter

private class SetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.Set[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object UnsortedSetDeserializer {
  val COMPANIONS = new CompanionSorter[collection.Set]()
    .add(immutable.HashSet)
    .add(immutable.ListSet)
    .add(immutable.Set)
    .add(mutable.HashSet)
    .add(mutable.LinkedHashSet)
    .add(mutable.Set)
    .toList

  def companionFor(cls: Class[_]): GenericCompanion[collection.Set] =
    COMPANIONS find { _._1.isAssignableFrom(cls) } map { _._2 } getOrElse (Set)

  def builderFor[A](cls: Class[_]): mutable.Builder[A, collection.Set[A]] = companionFor(cls).newBuilder[A]
}

private class SetInstantiator(config: DeserializationConfig, valueType: Class[_])
  extends StdValueInstantiator(config, valueType) {
  
  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new SetBuilderWrapper[AnyRef](UnsortedSetDeserializer.builderFor[AnyRef](valueType))

}

private class UnsortedSetDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[collection.Set[_]](collectionType.getRawClass)
  with ContextualDeserializer {

  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property).asInstanceOf[CollectionDeserializer]
    new UnsortedSetDeserializer(collectionType, newDelegate)
  }

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): collection.Set[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: SetBuilderWrapper[_] => wrapper.builder.result()
    }
}

private object UnsortedSetDeserializerResolver extends Deserializers.Base {

  lazy final val SET = classOf[collection.Set[_]]
  
  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SET.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SetInstantiator(config, rawClass)
      new UnsortedSetDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
    
  }
}

trait UnsortedSetDeserializerModule extends SetTypeModifierModule {
  this += (_ addDeserializers UnsortedSetDeserializerResolver)
}