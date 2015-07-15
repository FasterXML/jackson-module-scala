package com.fasterxml.jackson
package module.scala
package ser

import core.JsonGenerator

import databind.{BeanDescription, BeanProperty, JavaType, JsonSerializer, SerializationConfig, SerializerProvider}
import databind.`type`.CollectionLikeType
import databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.{impl, Serializers}
import databind.ser.std.AsArraySerializerBase

import modifiers.IteratorTypeModifierModule

import scala.collection.JavaConverters._

import java.{lang => jl}

private trait IteratorSerializer
  extends AsArraySerializerBase[collection.Iterator[Any]]
{
  def iteratorSerializer: impl.IteratorSerializer

  override def hasSingleElement(p1: collection.Iterator[Any]) =
    p1.hasDefiniteSize && p1.size == 1

  def serializeContents(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider) {
    iteratorSerializer.serializeContents(value.asJava, jgen, provider)
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_], unwrapSingle: jl.Boolean) =
    new ResolvedIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)


  override def isEmpty(value: collection.Iterator[Any]): Boolean = value.hasNext
}

private class ResolvedIteratorSerializer( src: IteratorSerializer,
                                          property: BeanProperty,
                                          vts: TypeSerializer,
                                          elementSerializer: JsonSerializer[_],
                                          unwrapSingle: jl.Boolean )
  extends AsArraySerializerBase[collection.Iterator[Any]](src, property, vts, elementSerializer, unwrapSingle)
  with IteratorSerializer
{
  val iteratorSerializer =
    new impl.IteratorSerializer(src.iteratorSerializer, property, vts, elementSerializer, unwrapSingle)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new ResolvedIteratorSerializer(src, property, newVts, elementSerializer, unwrapSingle)
}

private class UnresolvedIteratorSerializer( cls: Class[_],
                                            et: JavaType,
                                            staticTyping: Boolean,
                                            vts: TypeSerializer,
                                            elementSerializer: JsonSerializer[AnyRef] )
  extends AsArraySerializerBase[collection.Iterator[Any]](cls, et, staticTyping, vts, elementSerializer)
  with IteratorSerializer
{
  val iteratorSerializer =
    new impl.IteratorSerializer(et, staticTyping, vts)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new UnresolvedIteratorSerializer(cls, et, staticTyping, newVts, elementSerializer)
}

private object ScalaIteratorSerializerResolver extends Serializers.Base {
  override def findCollectionLikeSerializer(config: SerializationConfig,
                                            collectionType: CollectionLikeType,
                                            beanDescription: BeanDescription,
                                            elementTypeSerializer: TypeSerializer,
                                            elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    
    val rawClass = collectionType.getRawClass
    if (classOf[collection.Iterator[Any]].isAssignableFrom(rawClass))
      new UnresolvedIteratorSerializer(rawClass, collectionType.containedType(0), false, elementTypeSerializer, elementSerializer)       
    else
      null
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  this += ScalaIteratorSerializerResolver
}


