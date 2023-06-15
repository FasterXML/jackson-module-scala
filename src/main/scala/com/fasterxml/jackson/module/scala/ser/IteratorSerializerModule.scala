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

  override def hasSingleElement(p1: collection.Iterator[Any]): Boolean =
    p1.size == 1

  def serializeContents(value: collection.Iterator[Any], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    iteratorSerializer.serializeContents(value, jgen, provider)
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_], unwrapSingle: jl.Boolean) =
    new ResolvedIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)


  override def isEmpty(serializerProvider: SerializerProvider, value: collection.Iterator[Any]): Boolean = value.hasNext
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
