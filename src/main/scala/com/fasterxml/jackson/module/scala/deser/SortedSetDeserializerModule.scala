package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.SetTypeModifierModule
import com.fasterxml.jackson.databind.deser.{ValueInstantiator, ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.deser.std.{StdValueInstantiator, CollectionDeserializer, ContainerDeserializerBase}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.core.JsonParser
import scala.collection.{immutable, SortedSet, mutable}
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import java.util.AbstractCollection
import com.fasterxml.jackson.module.scala.introspect.OrderingLocator
import java.lang.Object

private class SortedSetBuilderWrapper[E](val builder: mutable.Builder[E, _ <: collection.SortedSet[E]]) extends AbstractCollection[E] {

  override def add(e: E) = { builder += e; true }

  // Required by AbstractCollection, but the deserializer doesn't care about them.
  def iterator() = null
  def size() = 0
}

private object SortedSetDeserializer {
  def orderingFor = OrderingLocator.locate _

  def builderFor(cls: Class[_], valueType: JavaType): mutable.Builder[AnyRef, SortedSet[AnyRef]] =
    if (classOf[mutable.TreeSet[_]].isAssignableFrom(cls)) mutable.TreeSet.newBuilder[AnyRef](orderingFor(valueType)) else
    if (classOf[mutable.SortedSet[_]].isAssignableFrom(cls)) mutable.SortedSet.newBuilder[AnyRef](orderingFor(valueType)) else
    if (classOf[immutable.TreeSet[_]].isAssignableFrom(cls)) immutable.TreeSet.newBuilder[AnyRef](orderingFor(valueType)) else
    immutable.SortedSet.newBuilder[AnyRef](orderingFor(valueType))
}

private class SortedSetInstantiator(config: DeserializationConfig, collectionType: Class[_], valueType: JavaType)
  extends StdValueInstantiator(config, collectionType) {

  override def canCreateUsingDefault = true

  override def createUsingDefault(ctxt: DeserializationContext) =
    new SortedSetBuilderWrapper[AnyRef](SortedSetDeserializer.builderFor(collectionType, valueType))
}


private class SortedSetDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
  extends ContainerDeserializerBase[collection.SortedSet[_]](collectionType)
  with ContextualDeserializer
{
  def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
    this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newDelegate = containerDeserializer.createContextual(ctxt, property)
    new SortedSetDeserializer(collectionType, newDelegate)
  }

  override def getContentType = containerDeserializer.getContentType

  override def getContentDeserializer = containerDeserializer.getContentDeserializer

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): collection.SortedSet[_] =
    containerDeserializer.deserialize(jp, ctxt) match {
      case wrapper: SortedSetBuilderWrapper[_] => wrapper.builder.result()
    }
}

private object SortedSetDeserializerResolver extends Deserializers.Base {
  private final val SORTED_SET = classOf[collection.SortedSet[_]]

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!SORTED_SET.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new SortedSetInstantiator(config, rawClass, collectionType.containedType(0))
      new SortedSetDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

}

trait SortedSetDeserializerModule extends SetTypeModifierModule {
  this += (_ addDeserializers SortedSetDeserializerResolver)
}
