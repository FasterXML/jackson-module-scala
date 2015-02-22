package com.fasterxml.jackson.module.scala.ser

import scala.collection.JavaConverters._

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer

import com.fasterxml.jackson.module.scala.modifiers.IteratorTypeModifierModule

private class ScalaIteratorSerializer(seqType: Class[_],
				      elemType: JavaType,
				      staticTyping: Boolean,
				      vts: Option[TypeSerializer],
				      property: BeanProperty,
				      valueSerializer: Option[JsonSerializer[AnyRef]]) extends
AsArraySerializerBase[collection.Iterator[Any]](seqType, elemType,
                                                staticTyping, vts.orNull,
                                                property,
                                                valueSerializer.orNull) {
  
  def hasSingleElement(p1: collection.Iterator[Any]) = false
  
  val iteratorSerializer = {
    // TODO: this looks weird, but there is no matching constructor in                                                                                                                                       
    // IteratorSerializer                                                                                                                                                                                    
    val s1 = new IteratorSerializer(elemType, staticTyping, vts.orNull, property)
    if (valueSerializer.isDefined) {
      s1.withResolved(property, vts.orNull, valueSerializer.orNull)
    }
    s1
  }

  def serializeContents(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider) {
    iteratorSerializer.serializeContents(value.asJava, jgen, provider)
  }

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    withResolved(property, newVts, valueSerializer.asInstanceOf[JsonSerializer[_]])
  
  override def withResolved(newProperty: BeanProperty, newVts: TypeSerializer, elementSerializer: JsonSerializer[_]) =
    new ScalaIteratorSerializer(seqType, elemType, staticTyping, Option(newVts), newProperty, Option(elementSerializer.asInstanceOf[JsonSerializer[AnyRef]]))
  
  override def isEmpty(value: collection.Iterator[Any]): Boolean = value.hasNext
}

private object ScalaIteratorSerializerResolver extends Serializers.Base {
  override def findCollectionLikeSerializer(config: SerializationConfig,
                                            collectionType: CollectionLikeType,
                                            beanDescription: BeanDescription,
                                            elementTypeSerializer: TypeSerializer,
                                            elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    
    val rawClass = collectionType.getRawClass
    if (classOf[collection.Iterator[Any]].isAssignableFrom(rawClass))
      new ScalaIteratorSerializer(rawClass, collectionType.containedType(0),
                                  false, Option(elementTypeSerializer), null,
                                  Option(elementSerializer))
    else
      null
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  this += ScalaIteratorSerializerResolver
}


