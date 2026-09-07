package com.fasterxml.jackson
package module.scala
package ser

import java.{lang => jl}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.modifiers.IteratorTypeModifierModule

private trait IteratorSerializer
  extends AsArraySerializerBase[collection.Iterator[Any]]
{
  def iteratorSerializer: ScalaIteratorSerializer

  // An iterator cannot be counted without being consumed, so this cannot be answered without
  // destroying the value it is asked about - the same reason databind's own IteratorSerializer
  // answers false here. WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED is honoured in serialize instead, from a
  // single element held back rather than from a length.
  override def hasSingleElement(p1: collection.Iterator[Any]): Boolean = false

  // AsArraySerializerBase.serialize would otherwise ask hasSingleElement whenever the feature is on
  override def serialize(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) {
      serializeUnwrappingSingle(value, jgen, provider)
    } else {
      writeArray(value, jgen, provider)
    }
  }

  // Pulls one element to find out whether a second follows, then serializes that element and the
  // rest of the iterator together - so nothing is lost whichever way the answer goes.
  private def serializeUnwrappingSingle(value: collection.Iterator[Any], jgen: JsonGenerator,
                                        provider: SerializerProvider): Unit = {
    if (!value.hasNext) writeArray(value, jgen, provider)
    else {
      val first = value.next()
      if (value.hasNext) writeArray(Iterator.single(first) ++ value, jgen, provider)
      else serializeContents(Iterator.single(first), jgen, provider)
    }
  }

  private def writeArray(value: collection.Iterator[Any], jgen: JsonGenerator,
                         provider: SerializerProvider): Unit = {
    jgen.writeStartArray(value)
    serializeContents(value, jgen, provider)
    jgen.writeEndArray()
  }

  def serializeContents(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    iteratorSerializer.serializeContents(value, jgen, provider)
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_], unwrapSingle: jl.Boolean) =
    new ResolvedIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)


  // hasNext leaves the iterator where it found it, so this is safe to ask before serializing
  override def isEmpty(serializerProvider: SerializerProvider, value: collection.Iterator[Any]): Boolean = !value.hasNext
}

private class ResolvedIteratorSerializer( src: IteratorSerializer,
                                          property: BeanProperty,
                                          vts: TypeSerializer,
                                          elementSerializer: JsonSerializer[_],
                                          unwrapSingle: jl.Boolean )
  extends AsArraySerializerBase[collection.Iterator[Any]](src, property, vts, elementSerializer, unwrapSingle)
  with IteratorSerializer {

  val iteratorSerializer =
    new ScalaIteratorSerializer(src.iteratorSerializer, property, vts, elementSerializer, unwrapSingle)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new ResolvedIteratorSerializer(src, property, newVts, elementSerializer, unwrapSingle)
}

private class UnresolvedIteratorSerializer( cls: Class[_],
                                            et: JavaType,
                                            staticTyping: Boolean,
                                            vts: TypeSerializer,
                                            elementSerializer: JsonSerializer[AnyRef] )
  extends AsArraySerializerBase[collection.Iterator[Any]](cls, et, staticTyping, vts, elementSerializer)
  with IteratorSerializer {

  override val iteratorSerializer =
    new ScalaIteratorSerializer(et, staticTyping, vts)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new UnresolvedIteratorSerializer(cls, et, staticTyping, newVts, elementSerializer)
}

private object ScalaIteratorSerializerResolver extends Serializers.Base {
  private val JSONSERIALIZABLE_CLASS = classOf[JsonSerializable]
  private val SCALAITERATOR_CLASS = classOf[collection.Iterator[_]]

  override def findCollectionLikeSerializer(config: SerializationConfig,
                                            collectionType: CollectionLikeType,
                                            beanDescription: BeanDescription,
                                            elementTypeSerializer: TypeSerializer,
                                            elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

    val rawClass = collectionType.getRawClass
    if (!SCALAITERATOR_CLASS.isAssignableFrom(rawClass) || JSONSERIALIZABLE_CLASS.isAssignableFrom(rawClass)) None.orNull
    else new UnresolvedIteratorSerializer(rawClass, collectionType.getContentType, false, elementTypeSerializer, elementSerializer)
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  override def getModuleName: String = "IteratorSerializerModule"
  this += ScalaIteratorSerializerResolver
}
