package com.fasterxml.jackson
package module.scala
package ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.module.scala.modifiers.IteratorTypeModifierModule

import java.{lang => jl}

private trait IteratorSerializer
  extends AsArraySerializerBase[collection.Iterator[Any]]
{
  def iteratorSerializer: ScalaIteratorSerializer

  override def hasSingleElement(p1: collection.Iterator[Any]): Boolean =
    p1.size == 1

  override def serialize(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED) && hasSingleElement(value)) {
      iteratorSerializer.serializeContents(value, jgen, provider)
    } else {
      jgen.writeStartArray(value)
      iteratorSerializer.serializeContents(value, jgen, provider)
      jgen.writeEndArray()
    }
  }

  override def serializeContents(value: collection.Iterator[Any], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    serialize(value, gen, provider)
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: ValueSerializer[_], unwrapSingle: jl.Boolean) =
    new ResolvedIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)


  override def isEmpty(provider: SerializerProvider, value: collection.Iterator[Any]): Boolean = value.hasNext
}

private class ResolvedIteratorSerializer( src: IteratorSerializer,
                                          property: BeanProperty,
                                          vts: TypeSerializer,
                                          elementSerializer: ValueSerializer[_],
                                          unwrapSingle: jl.Boolean )
  extends AsArraySerializerBase[collection.Iterator[Any]](src, vts, elementSerializer, unwrapSingle, property)
  with IteratorSerializer
{
  val iteratorSerializer =
    new ScalaIteratorSerializer(src.iteratorSerializer, property, vts, elementSerializer, unwrapSingle)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new ResolvedIteratorSerializer(src, property, newVts, elementSerializer, unwrapSingle)
}

private class UnresolvedIteratorSerializer( cls: Class[_],
                                            et: JavaType,
                                            staticTyping: Boolean,
                                            vts: TypeSerializer,
                                            elementSerializer: ValueSerializer[AnyRef] )
  extends AsArraySerializerBase[collection.Iterator[Any]](cls, et, staticTyping, vts, elementSerializer)
  with IteratorSerializer
{
  val iteratorSerializer =
    new ScalaIteratorSerializer(et, staticTyping, vts)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new UnresolvedIteratorSerializer(cls, et, staticTyping, newVts, elementSerializer)
}

private class ScalaIteratorSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findCollectionLikeSerializer(serializationConfig: SerializationConfig,
                                            collectionType: CollectionLikeType,
                                            beanDescription: BeanDescription,
                                            formatOverrides: JsonFormat.Value,
                                            elementTypeSerializer: TypeSerializer,
                                            elementSerializer: ValueSerializer[Object]): ValueSerializer[_] = {

    val rawClass = collectionType.getRawClass
    if (!classOf[collection.Iterator[Any]].isAssignableFrom(rawClass)) null else
    new UnresolvedIteratorSerializer(rawClass, collectionType.getContentType, false, elementTypeSerializer, elementSerializer)
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  override def initScalaModule(config: ScalaModule.Config): Unit = {
    this += new ScalaIteratorSerializerResolver(config)
  }
}
