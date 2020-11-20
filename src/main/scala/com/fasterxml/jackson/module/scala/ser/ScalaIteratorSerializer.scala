package com.fasterxml.jackson.module.scala.ser

import java.{lang => jl}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.ContainerSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind._

import scala.util.control.NonFatal

private case class ScalaIteratorSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                                           property: BeanProperty, valueSerializer: JsonSerializer[Object], unwrapSingle: jl.Boolean)
  extends AsArraySerializerBase[collection.Iterator[Any]](collection.Iterator.getClass, elemType, staticTyping, vts, property, valueSerializer, unwrapSingle) {

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer) = {
    this(elemType, staticTyping, vts, None.orNull, None.orNull, None.orNull)
  }

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: JsonSerializer[Object]) = {
    this(elemType, staticTyping, vts, None.orNull, valueSerializer.asInstanceOf[JsonSerializer[Object]], None.orNull)
  }

  def this(src: ScalaIteratorSerializer, property: BeanProperty, vts: TypeSerializer, valueSerializer: JsonSerializer[_],
           unwrapSingle: jl.Boolean) = {
    this(src.elemType, src.staticTyping, vts, property, valueSerializer.asInstanceOf[JsonSerializer[Object]], unwrapSingle)
  }

  override def isEmpty(prov: SerializerProvider, value: Iterator[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterator[Any]): Boolean = value.size == 1

  override def serialize(value: Iterator[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    //writeSingleElement is unsupported - also unsupported in com.fasterxml.jackson.databind.ser.impl.IteratorSerializer
    //calculating the length of iterators can be expensive
    g.writeStartArray(value)
    serializeContents(value, g, provider)
    g.writeEndArray()
  }

  override def serializeContents(it: Iterator[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    g.setCurrentValue(it)
    if (_elementSerializer != null) {
      serializeContentsUsing(it, g, provider, _elementSerializer)
    } else {
      if (it.hasNext) {
        val typeSer = _valueTypeSerializer
        var serializers = _dynamicSerializers
        var i = 0
        try while (it.hasNext) {
          val elem = it.next()
          if (elem == null) provider.defaultSerializeNull(g)
          else {
            val cc = elem.getClass
            var serializer = serializers.serializerFor(cc)
            if (serializer == null) {
              if (_elementType.hasGenericTypes) serializer = _findAndAddDynamic(serializers, provider.constructSpecializedType(_elementType, cc), provider)
              else serializer = _findAndAddDynamic(serializers, cc, provider)
              serializers = _dynamicSerializers
            }
            if (typeSer == null) serializer.serialize(elem.asInstanceOf[Object], g, provider)
            else serializer.serializeWithType(elem.asInstanceOf[Object], g, provider, typeSer)
          }
          i += 1
        }
        catch {
          case NonFatal(e) =>
            wrapAndThrow(provider, e, it, i)
        }
      }
    }
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_],
                            unwrapSingle: jl.Boolean): AsArraySerializerBase[Iterator[Any]] = {
    new ScalaIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)
  }

  override def _withValueTypeSerializer(vts: TypeSerializer): ContainerSerializer[_] = {
    new ScalaIteratorSerializer(this, _property, vts, _elementSerializer, _unwrapSingle)
  }

  private def serializeContentsUsing(it: Iterator[Any], g: JsonGenerator, provider: SerializerProvider, ser: JsonSerializer[AnyRef]): Unit = {
    if (it.hasNext) {
      val typeSer = _valueTypeSerializer
      var i = 0
      while (it.hasNext) {
        val elem = it.next()
        try {
          if (elem == null) provider.defaultSerializeNull(g)
          else if (typeSer == null) ser.serialize(elem.asInstanceOf[Object], g, provider)
          else ser.serializeWithType(elem.asInstanceOf[Object], g, provider, typeSer)
          i += 1
        } catch {
          case NonFatal(e) =>
            wrapAndThrow(provider, e, it, i)
        }
      }
    }
  }
}