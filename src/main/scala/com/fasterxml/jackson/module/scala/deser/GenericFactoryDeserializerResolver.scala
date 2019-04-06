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
  type Collection[A] = CC[A]
  type Factory = CF[CC]
  type Builder[A] = mutable.Builder[A, _ <: Collection[A]]

  // Subclasses need to implement the following:
  val CLASS_DOMAIN: Class[Collection[_]]
  val factories: Iterable[(Class[_], Factory)]
  def builderFor[A](cf: Factory, valueType: JavaType): Builder[A]

  def builderFor[A](cls: Class[_], valueType: JavaType): Builder[A] = factories
    .find(_._1.isAssignableFrom(cls))
    .map(_._2)
    .map(builderFor[A](_, valueType))
    .getOrElse(throw new IllegalStateException(s"Could not find deserializer for ${cls.getCanonicalName}. File issue on github:fasterxml/jackson-scala-module."))

  override def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!CLASS_DOMAIN.isAssignableFrom(collectionType.getRawClass)) null
    else {
      val deser = elementDeserializer.asInstanceOf[JsonDeserializer[AnyRef]]
      val instantiator = new Instantiator(config, collectionType, collectionType.getContentType)
      new Deserializer(collectionType, deser, elementTypeDeserializer, instantiator)
    }
  }

  private class BuilderWrapper[A](val builder: Builder[A]) extends util.AbstractCollection[A] {
    var size = 0

    override def add(e: A): Boolean = { builder += e; size += 1; true }

    // Required by AbstractCollection, but not implemented
    override def iterator(): util.Iterator[A] = null
  }

  private class Instantiator(config: DeserializationConfig, collectionType: JavaType, valueType: JavaType)
    extends StdValueInstantiator(config, collectionType) {

    override def canCreateUsingDefault = true

    override def createUsingDefault(ctxt: DeserializationContext) =
      new BuilderWrapper[AnyRef](builderFor[AnyRef](collectionType.getRawClass, valueType))
  }

  private class Deserializer(collectionType: JavaType, containerDeserializer: CollectionDeserializer)
    extends ContainerDeserializerBase[CC[_]](collectionType)
      with ContextualDeserializer {

    def this(collectionType: JavaType, valueDeser: JsonDeserializer[Object], valueTypeDeser: TypeDeserializer, valueInstantiator: ValueInstantiator) {
      this(collectionType, new CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, valueInstantiator))
    }

    override def createContextual(ctxt: DeserializationContext, property: BeanProperty): Deserializer = {
      val newDelegate = containerDeserializer.createContextual(ctxt, property)
      new Deserializer(collectionType, newDelegate)
    }

    override def getContentType: JavaType = containerDeserializer.getContentType

    override def getContentDeserializer: JsonDeserializer[AnyRef] = containerDeserializer.getContentDeserializer

    override def deserialize(jp: JsonParser, ctxt: DeserializationContext): CC[_] =
      containerDeserializer.deserialize(jp, ctxt) match {
        case wrapper: BuilderWrapper[_] => wrapper.builder.result().asInstanceOf[CC[_]]
      }

    // Crazy workaround for https://github.com/scala/scala-dev/issues/623
    override def getNullValue(ctx: DeserializationContext): CC[_] with Object = super.getNullValue.asInstanceOf[CC[_] with Object]
  }
}
