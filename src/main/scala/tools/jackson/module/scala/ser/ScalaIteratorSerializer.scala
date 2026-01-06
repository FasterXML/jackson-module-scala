package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind._
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.std.{AsArraySerializerBase, StdContainerSerializer}

import java.{lang => jl}
import scala.util.control.NonFatal

private case class ScalaIteratorSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                                           property: BeanProperty, valueSerializer: ValueSerializer[Object],
                                           unwrapSingle: jl.Boolean, suppressableValue: Any, suppressNulls: Boolean)
  extends AsArraySerializerBase[collection.Iterator[Any]](collection.Iterator.getClass, elemType, staticTyping, vts, valueSerializer,
    unwrapSingle, property, suppressableValue, suppressNulls) {

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer) = {
    this(elemType, staticTyping, vts, property = None.orNull, valueSerializer = None.orNull,
      unwrapSingle = None.orNull, suppressableValue = None.orNull, suppressNulls = false)
  }

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: ValueSerializer[Object]) = {
    this(elemType, staticTyping, vts, property = None.orNull, valueSerializer, unwrapSingle = None.orNull,
      suppressableValue = None.orNull, suppressNulls = false)
  }

  def this(src: ScalaIteratorSerializer, property: BeanProperty, vts: TypeSerializer, valueSerializer: ValueSerializer[_],
           unwrapSingle: jl.Boolean, suppressableValue: Any, suppressNulls: Boolean) = {
    this(src.elemType, src.staticTyping, vts, property, valueSerializer.asInstanceOf[ValueSerializer[Object]],
      unwrapSingle, suppressableValue = suppressableValue, suppressNulls = suppressNulls)
  }

  override def isEmpty(prov: SerializationContext, value: Iterator[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterator[Any]): Boolean = value.size == 1

  override def serialize(value: Iterator[Any], g: JsonGenerator, serializationContext: SerializationContext): Unit = {
    //writeSingleElement is unsupported - also unsupported in tools.jackson.databind.ser.impl.IteratorSerializer
    //calculating the length of iterators can be expensive
    g.writeStartArray(value)
    serializeContents(value, g, serializationContext)
    g.writeEndArray()
  }

  override def serializeContents(it: Iterator[Any], g: JsonGenerator, serializationContext: SerializationContext): Unit = {
    g.assignCurrentValue(it)
    if (_elementSerializer != null) {
      serializeContentsUsing(it, g, serializationContext, _elementSerializer)
    } else {
      val needsFiltering = _needToCheckFiltering(serializationContext)
      if (it.hasNext) {
        val typeSer = _valueTypeSerializer
        var serializers = _dynamicValueSerializers
        var i = 0
        try while (it.hasNext) {
          val elem = it.next()
          if (elem == null) {
            if (needsFiltering && _suppressNulls) {
              // skip
            } else {
              serializationContext.defaultSerializeNullValue(g)
              i += 1
            }
          } else {
            val cc = elem.getClass
            var serializer = serializers.serializerFor(cc)
            if (serializer == null) {
              if (_elementType.hasGenericTypes)
                serializer = _findAndAddDynamic(serializationContext, serializationContext.constructSpecializedType(_elementType, cc))
              else
                serializer = _findAndAddDynamic(serializationContext, cc)
              serializers = _dynamicValueSerializers
            }
            if (needsFiltering && !_shouldSerializeElement(serializationContext, elem, serializer)) {
              // skip
            } else if (typeSer == null) {
              serializer.serialize(elem.asInstanceOf[Object], g, serializationContext)
              i += 1
            } else {
              serializer.serializeWithType(elem.asInstanceOf[Object], g, serializationContext, typeSer)
              i += 1
            }
          }
        }
        catch {
          case NonFatal(e) =>
            wrapAndThrow(serializationContext, e, it, i)
        }
      }
    }
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: ValueSerializer[_],
                            unwrapSingle: jl.Boolean, suppressableValue: Any,
                            suppressNulls: Boolean): AsArraySerializerBase[Iterator[Any]] =
    new ScalaIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle = unwrapSingle,
      suppressableValue = suppressableValue, suppressNulls = suppressNulls)

  override def _withValueTypeSerializer(vts: TypeSerializer): StdContainerSerializer[_] =
    new ScalaIteratorSerializer(this, _property, vts, _elementSerializer, unwrapSingle = _unwrapSingle,
      suppressableValue = _suppressableValue, suppressNulls = _suppressNulls)

  private def serializeContentsUsing(it: Iterator[Any], g: JsonGenerator, serializationContext: SerializationContext, ser: ValueSerializer[AnyRef]): Unit = {
    val needsFiltering = _needToCheckFiltering(serializationContext)
    if (it.hasNext) {
      val typeSer = _valueTypeSerializer
      var i = 0
      while (it.hasNext) {
        val elem = it.next()
        if (elem == null) {
          if (needsFiltering && _suppressNulls) {
            // skip
          } else {
            serializationContext.defaultSerializeNullValue(g)
            i += 1
          }
        } else {
          if (needsFiltering && !_shouldSerializeElement(serializationContext, elem, _elementSerializer)) {
            // skip
          } else if (typeSer == null) {
            ser.serialize(elem.asInstanceOf[Object], g, serializationContext)
            i += 1
          } else {
            ser.serializeWithType(elem.asInstanceOf[Object], g, serializationContext, typeSer)
            i += 1
          }
        }
      }
    }
  }
}
