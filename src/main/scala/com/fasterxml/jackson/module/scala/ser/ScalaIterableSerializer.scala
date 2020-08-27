package com.fasterxml.jackson.module.scala.ser

import java.lang

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.ContainerSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.{BeanProperty, JavaType, JsonSerializer, SerializationFeature, SerializerProvider}

case class ScalaIterableSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                              property: BeanProperty, valueSerializer: JsonSerializer[Object])
  extends AsArraySerializerBase[collection.Iterable[Any]](collection.Iterable.getClass, elemType, staticTyping, vts, property, valueSerializer) {

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: JsonSerializer[Object]) {
    this(elemType, staticTyping, vts, None.orNull, valueSerializer.asInstanceOf[JsonSerializer[Object]])
  }

  def this(src: ScalaIterableSerializer, property: BeanProperty, vts: TypeSerializer, valueSerializer: JsonSerializer[_]) {
    this(src.elemType, src.staticTyping, vts, property, valueSerializer.asInstanceOf[JsonSerializer[Object]])
  }

  override def isEmpty(prov: SerializerProvider, value: Iterable[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterable[Any]): Boolean = value.size == 1

  override def serialize(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    val len: Int = value.size
    if (len == 1) {
      if (((_unwrapSingle == null) && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) || (_unwrapSingle)) {
        serializeContents(value, g, provider)
        return
      }
    }
    g.writeStartArray(value, len)
    serializeContents(value, g, provider)
    g.writeEndArray()
  }

  override def serializeContents(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    g.setCurrentValue(value)
    if (_elementSerializer != null) {
      serializeContentsUsing(value, g, provider, _elementSerializer)
      return
    }
    val it = value.iterator
    if (!it.hasNext) return
    var serializers = _dynamicSerializers
    val typeSer = _valueTypeSerializer
    var i = 0
    try do {
      val elem = it.next
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
    } while ( {
      it.hasNext
    })
    catch {
      case e: Exception =>
        wrapAndThrow(provider, e, value, i)
    }
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: JsonSerializer[_],
                            unwrapSingle: lang.Boolean): AsArraySerializerBase[Iterable[Any]] = {
    new ScalaIterableSerializer(this, property, vts, elementSerializer)
  }

  override def _withValueTypeSerializer(vts: TypeSerializer): ContainerSerializer[_] = {
    new ScalaIterableSerializer(this, _property, vts, _elementSerializer)
  }

  private def serializeContentsUsing(value: Iterable[Any], g: JsonGenerator, provider: SerializerProvider, ser: JsonSerializer[AnyRef]): Unit = {
    val it = value.iterator
    if (it.hasNext) {
      val typeSer = _valueTypeSerializer
      var i = 0
      do {
        val elem = it.next
        try {
          if (elem == null) provider.defaultSerializeNull(g)
          else if (typeSer == null) ser.serialize(elem.asInstanceOf[Object], g, provider)
          else ser.serializeWithType(elem.asInstanceOf[Object], g, provider, typeSer)
          i += 1
        } catch {
          case e: Exception =>
            wrapAndThrow(provider, e, value, i)
        }
      } while ( {
        it.hasNext
      })
    }
  }
}