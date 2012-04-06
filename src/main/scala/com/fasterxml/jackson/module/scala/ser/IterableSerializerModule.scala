package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.{AsArraySerializerBase, CollectionSerializer};

import com.fasterxml.jackson.module.scala.modifiers.IterableTypeModifierModule
import collection.Iterable

private class IterableSerializer(seqType: Class[_],
                                 elemType: JavaType,
                                 staticTyping: Boolean,
                                 vts: Option[TypeSerializer],
                                 property: BeanProperty,
                                 valueSerializer: Option[JsonSerializer[AnyRef]])
  extends AsArraySerializerBase[collection.Iterable[Any]](seqType, elemType, staticTyping, vts.orNull, property, valueSerializer.orNull) {

  def hasSingleElement(p1: Iterable[Any]) = (p1.take(2).size == 1)

  val collectionSerializer =
    new CollectionSerializer(elemType, staticTyping, vts.orNull, property, valueSerializer.orNull)

  def serializeContents(value: Iterable[Any], jgen: JsonGenerator, provider: SerializerProvider) {
    collectionSerializer.serializeContents(value.asJavaCollection, jgen, provider)
  }

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    withResolved(property, newVts, valueSerializer.asInstanceOf[JsonSerializer[_]])

  override def withResolved(newProperty: BeanProperty, newVts: TypeSerializer, elementSerializer: JsonSerializer[_]) =
    new IterableSerializer(seqType, elemType, staticTyping, Option(newVts), newProperty, Option(elementSerializer.asInstanceOf[JsonSerializer[AnyRef]]))
}

private object IterableSerializerResolver extends Serializers.Base {

  override def findCollectionLikeSerializer(config: SerializationConfig,
                   collectionType: CollectionLikeType,
                   beanDescription: BeanDescription,
                   elementTypeSerializer: TypeSerializer,
                   elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    val rawClass = collectionType.getRawClass
    if (!classOf[collection.Iterable[Any]].isAssignableFrom(rawClass)) null else
    if (classOf[collection.Map[Any,Any]].isAssignableFrom(rawClass)) null else
    new IterableSerializer(rawClass, collectionType.containedType(0), false, Option(elementTypeSerializer), null, Option(elementSerializer))
  }

}

trait IterableSerializerModule extends IterableTypeModifierModule {
  this += IterableSerializerResolver
}