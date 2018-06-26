package com.fasterxml.jackson.module.scala.deser

import java.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.deser.std.{CollectionDeserializer, ContainerDeserializerBase, StdValueInstantiator}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers, ValueInstantiator}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer

import scala.collection.mutable
import scala.language.higherKinds

abstract class GenericFactoryDeserializerResolver[CC[_], CF[X[_]]] extends Deserializers.Base {
  type Collection[X] = CC[X]
  type Factory = CF[CC]

  // Subclasses need to implement the following:
  val CLASS_DOMAIN: Class[Collection[_]]
  val factories: Iterable[(Class[_], Factory)]
  def builderFor[A](cf: Factory): mutable.Builder[A, Collection[A]]

  private def factoryFor(cls: Class[_]): CF[CC] = factories
    .find(_._1.isAssignableFrom(cls))
    .map(_._2)
    .getOrElse(throw new IllegalStateException(s"Could not find deserializer for ${cls.toString}. File issue on github:fasterxml/jackson-scala-module"))

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = collectionType.getRawClass

    if (!CLASS_DOMAIN.isAssignableFrom(rawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new GenericFactoryInstantiator(config, collectionType)
      new GenericFactoryDeserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

  private class BuilderWrapper[E](val builder: mutable.Builder[E, _ <: CC[E]]) extends util.AbstractCollection[E] {
    var size = 0

    override def add(e: E): Boolean = { builder += e; size += 1; true }

    // Required by AbstractCollection, but not implemented
    override def iterator(): util.Iterator[E] = null
  }

  private class GenericFactoryInstantiator(config: DeserializationConfig, valueType: JavaType)
    extends StdValueInstantiator(config, valueType) {

    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) =
      new BuilderWrapper[AnyRef](builderFor[AnyRef](factoryFor(valueType.getRawClass)))
  }

  private class GenericFactoryDeserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
    extends ContainerDeserializerBase[CC[_]](collectionType)
      with ContextualDeserializer {

    def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) =
      this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))

    def createContextual(ctxt: DeserializationContext, property: BeanProperty): GenericFactoryDeserializer = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property)
      new GenericFactoryDeserializer(collectionType, newDelegate)
    }

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): CC[_] =
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[CC[_]]
      }
  }
}
