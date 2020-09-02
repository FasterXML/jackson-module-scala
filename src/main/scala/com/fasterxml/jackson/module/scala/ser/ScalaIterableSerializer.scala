package com.fasterxml.jackson.module.scala.ser

import java.{lang => jl}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.ContainerSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.{BeanProperty, JavaType, JsonSerializer, SerializationFeature, SerializerProvider}

import scala.util.control.NonFatal

private case class ScalaIterableSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                                           valueSerializer: JsonSerializer[Object], unwrapSingle: jl.Boolean)
  extends AsArraySerializerBase[collection.Iterable[Any]](collection.Iterable.getClass, elemType, staticTyping, vts, valueSerializer, unwrapSingle) {

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: JsonSerializer[Object]) = {
    this(elemType, staticTyping, vts, valueSerializer.asInstanceOf[JsonSerializer[Object]], None.orNull)
  }

  def this(src: ScalaIterableSerializer, vts: TypeSerializer, valueSerializer: JsonSerializer[_], unwrapSingle: jl.Boolean) = {
    this(src.elemType, src.staticTyping, vts, valueSerializer.asInstanceOf[JsonSerializer[Object]], unwrapSingle)
  }

  override def isEmpty(prov: SerializerProvider, value: Iterable[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterable[Any]): Boolean = value.size == 1

  override def serialize(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    if (((_unwrapSingle == null && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
      || _unwrapSingle) && hasSingleElement(value)) {
      serializeContents(value, g, provider)
    } else {
      g.writeStartArray(value)
      serializeContents(value, g, provider)
      g.writeEndArray()
    }
  }

  override def serializeContents(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    g.setCurrentValue(value)
    if (_elementSerializer != null) {
      serializeContentsUsing(value, g, provider, _elementSerializer)
    } else {
      val it = value.iterator
      if (it.hasNext) {
        val typeSer = _valueTypeSerializer
        var serializers = _dynamicValueSerializers
        var i = 0
        try do {
          val elem = it.next()
          if (elem == null) provider.defaultSerializeNullValue(g)
          else {
            val cc = elem.getClass
            var serializer = serializers.serializerFor(cc)
            if (serializer == null) {
              if (_elementType.hasGenericTypes) serializer = _findAndAddDynamic(provider, provider.constructSpecializedType(_elementType, cc))
              else serializer = _findAndAddDynamic(provider, cc)
              serializers = _dynamicValueSerializers
            }
            if (typeSer == null) serializer.serialize(elem.asInstanceOf[Object], g, provider)
            else serializer.serializeWithType(elem.asInstanceOf[Object], g, provider, typeSer)
          }
          i += 1
        } while (it.hasNext)
        catch {
          case NonFatal(e) =>
            wrapAndThrow(provider, e, value, i)
        }
      }
    }
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_],
                            unwrapSingle: jl.Boolean): AsArraySerializerBase[Iterable[Any]] = {
    new ScalaIterableSerializer(this, vts, elementSerializer, unwrapSingle)
  }

  override def _withValueTypeSerializer(vts: TypeSerializer): ContainerSerializer[_] = {
    new ScalaIterableSerializer(this, vts, _elementSerializer, _unwrapSingle)
  }

  private def serializeContentsUsing(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider, ser: JsonSerializer[AnyRef]): Unit = {
    val it = value.iterator
    if (it.hasNext) {
      val typeSer = _valueTypeSerializer
      var i = 0
      do {
        val elem = it.next()
        try {
          if (elem == null) provider.defaultSerializeNullValue(g)
          else if (typeSer == null) ser.serialize(elem.asInstanceOf[Object], g, provider)
          else ser.serializeWithType(elem.asInstanceOf[Object], g, provider, typeSer)
          i += 1
        } catch {
          case NonFatal(e) =>
            wrapAndThrow(provider, e, value, i)
        }
      } while (it.hasNext)
    }
  }
}