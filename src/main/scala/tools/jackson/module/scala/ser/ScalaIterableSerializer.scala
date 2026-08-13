package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.std.{AsArraySerializerBase, StdContainerSerializer}
import tools.jackson.databind._

import java.{lang => jl}

private case class ScalaIterableSerializer(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer,
                                           property: BeanProperty, valueSerializer: ValueSerializer[Object],
                                           unwrapSingle: jl.Boolean, suppressableValue: Any, suppressNulls: Boolean)
  extends AsArraySerializerBase[collection.Iterable[Any]](collection.Iterable.getClass, elemType, staticTyping, vts, valueSerializer,
    unwrapSingle, property, suppressableValue, suppressNulls) {

  private val iteratorSerializer = ScalaIteratorSerializer(elemType, staticTyping = staticTyping, vts,
    property, valueSerializer, unwrapSingle = unwrapSingle,
    suppressableValue = suppressableValue, suppressNulls = suppressNulls)

  def this(elemType: JavaType, staticTyping: Boolean, vts: TypeSerializer, valueSerializer: ValueSerializer[Object]) = {
    this(elemType, staticTyping, vts, property = null, valueSerializer, unwrapSingle = null,
      suppressableValue = null, suppressNulls = false)
  }

  def this(src: ScalaIterableSerializer, property: BeanProperty, vts: TypeSerializer, valueSerializer: ValueSerializer[_],
           unwrapSingle: jl.Boolean, suppressableValue: Any, suppressNulls: Boolean) = {
    this(src.elemType, src.staticTyping, vts, property, valueSerializer.asInstanceOf[ValueSerializer[Object]],
      unwrapSingle = unwrapSingle, suppressableValue = suppressableValue, suppressNulls = suppressNulls)
  }

  override def isEmpty(prov: SerializationContext, value: Iterable[Any]): Boolean = value.isEmpty

  override def hasSingleElement(value: Iterable[Any]): Boolean = value.size == 1

  override def serialize(value: Iterable[Any], g: JsonGenerator, serializationContext: SerializationContext): Unit =
    iteratorSerializer.serialize(value.iterator, g, serializationContext)

  override def serializeContents(value: Iterable[Any], g: JsonGenerator, serializationContext: SerializationContext): Unit =
    iteratorSerializer.serializeContents(value.iterator, g, serializationContext)

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: ValueSerializer[_],
                            unwrapSingle: jl.Boolean, suppressableValue: Any,
                            suppressNulls: Boolean): AsArraySerializerBase[Iterable[Any]] =
    new ScalaIterableSerializer(this, property, vts, elementSerializer, unwrapSingle = unwrapSingle,
      suppressableValue = suppressableValue, suppressNulls = suppressNulls)

  override def _withValueTypeSerializer(vts: TypeSerializer): StdContainerSerializer[_] =
    new ScalaIterableSerializer(this, _property, vts, _elementSerializer, unwrapSingle = _unwrapSingle,
      suppressableValue = _suppressableValue, suppressNulls = _suppressNulls)
}
