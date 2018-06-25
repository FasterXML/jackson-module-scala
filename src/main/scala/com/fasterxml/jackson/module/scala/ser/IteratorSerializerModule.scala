package com.fasterxml.jackson
package module.scala
package ser

import java.{lang => jl}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.ser.{Serializers, impl}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.modifiers.IteratorTypeModifierModule

import scala.collection.JavaConverters._

private trait IteratorSerializer
  extends AsArraySerializerBase[collection.Iterator[Any]]
{
  def iteratorSerializer: impl.IteratorSerializer

  override def hasSingleElement(p1: collection.Iterator[Any]): Boolean =
    p1.knownSize == 1

  def serializeContents(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
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
    if (!classOf[collection.Iterator[Any]].isAssignableFrom(rawClass)) null else
    new UnresolvedIteratorSerializer(rawClass, collectionType.getContentType, false, elementTypeSerializer, elementSerializer)
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  this += ScalaIteratorSerializerResolver
}
