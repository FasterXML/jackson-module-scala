package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind._
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.std.{AsArraySerializerBase, StdContainerSerializer}

import java.{lang => jl}
import scala.util.control.NonFatal

private case class ScalaIteratorSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                                           property: BeanProperty, valueSerializer: ValueSerializer[Object], unwrapSingle: jl.Boolean)
  extends AsArraySerializerBase[collection.Iterator[Any]](collection.Iterator.getClass, elemType, staticTyping, vts, valueSerializer, unwrapSingle, property) {

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer) = {
    this(elemType, staticTyping, vts, None.orNull, None.orNull, None.orNull)
  }

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: ValueSerializer[Object]) = {
    this(elemType, staticTyping, vts, None.orNull, valueSerializer.asInstanceOf[ValueSerializer[Object]], None.orNull)
  }

  def this(src: ScalaIteratorSerializer, property: BeanProperty, vts: TypeSerializer, valueSerializer: ValueSerializer[_],
           unwrapSingle: jl.Boolean) = {
    this(src.elemType, src.staticTyping, vts, property, valueSerializer.asInstanceOf[ValueSerializer[Object]], unwrapSingle)
  }

  override def isEmpty(prov: SerializerProvider, value: Iterator[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterator[Any]): Boolean = value.size == 1

  override def serialize(value: Iterator[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    //writeSingleElement is unsupported - also unsupported in tools.jackson.databind.ser.impl.IteratorSerializer
    //calculating the length of iterators can be expensive
    g.writeStartArray(value)
    serializeContents(value, g, provider)
    g.writeEndArray()
  }

  override def serializeContents(it: Iterator[Any], g: JsonGenerator, provider: SerializerProvider): Unit = {
    g.assignCurrentValue(it)
    if (_elementSerializer != null) {
      serializeContentsUsing(it, g, provider, _elementSerializer)
    } else {
      if (it.hasNext) {
        val typeSer = _valueTypeSerializer
        var serializers = _dynamicValueSerializers
        var i = 0
        try while (it.hasNext) {
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
        }
        catch {
          case NonFatal(e) =>
            wrapAndThrow(provider, e, it, i)
        }
      }
    }
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: ValueSerializer[_],
                            unwrapSingle: jl.Boolean): AsArraySerializerBase[Iterator[Any]] = {
    new ScalaIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle)
  }

  override def _withValueTypeSerializer(vts: TypeSerializer): StdContainerSerializer[_] = {
    new ScalaIteratorSerializer(this, _property, vts, _elementSerializer, _unwrapSingle)
  }

  private def serializeContentsUsing(it: Iterator[Any], g: JsonGenerator, provider: SerializerProvider, ser: ValueSerializer[AnyRef]): Unit = {
    if (it.hasNext) {
      val typeSer = _valueTypeSerializer
      var i = 0
      while (it.hasNext) {
        val elem = it.next()
        try {
          if (elem == null) provider.defaultSerializeNullValue(g)
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