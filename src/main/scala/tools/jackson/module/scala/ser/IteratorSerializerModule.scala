package tools.jackson
package module.scala
package ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.CollectionLikeType
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.std.AsArraySerializerBase
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.modifiers.IteratorTypeModifierModule

import java.{lang => jl}

private trait IteratorSerializer
  extends AsArraySerializerBase[collection.Iterator[Any]]
{
  def iteratorSerializer: ScalaIteratorSerializer

  // An iterator cannot be counted without being consumed, so this cannot be answered without
  // destroying the value it is asked about - the same reason databind's own IteratorSerializer
  // answers false here. WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED is honoured in serialize instead, from a
  // single element held back rather than from a length.
  override def hasSingleElement(p1: collection.Iterator[Any]): Boolean = false

  override def serialize(value: collection.Iterator[Any], jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    if (serializationContext.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)) {
      serializeUnwrappingSingle(value, jgen, serializationContext)
    } else {
      writeArray(value, jgen, serializationContext)
    }
  }

  // Pulls one element to find out whether a second follows, then serializes that element and the
  // rest of the iterator together - so nothing is lost whichever way the answer goes.
  private def serializeUnwrappingSingle(value: collection.Iterator[Any], jgen: JsonGenerator,
                                        serializationContext: SerializationContext): Unit = {
    if (!value.hasNext) writeArray(value, jgen, serializationContext)
    else {
      val first = value.next()
      if (value.hasNext) writeArray(Iterator.single(first) ++ value, jgen, serializationContext)
      else iteratorSerializer.serializeContents(Iterator.single(first), jgen, serializationContext)
    }
  }

  private def writeArray(value: collection.Iterator[Any], jgen: JsonGenerator,
                         serializationContext: SerializationContext): Unit = {
    jgen.writeStartArray(value)
    iteratorSerializer.serializeContents(value, jgen, serializationContext)
    jgen.writeEndArray()
  }

  override def serializeContents(value: collection.Iterator[Any], gen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    serialize(value, gen, serializationContext)
  }

  override def withResolved(property: BeanProperty, vts: TypeSerializer, elementSerializer: ValueSerializer[_],
                            unwrapSingle: jl.Boolean, suppressableValue: Any, suppressNulls: Boolean) =
    new ResolvedIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle,
      suppressableValue, suppressNulls)

  // hasNext leaves the iterator where it found it, so this is safe to ask before serializing
  override def isEmpty(serializationContext: SerializationContext, value: collection.Iterator[Any]): Boolean = !value.hasNext
}

private class ResolvedIteratorSerializer( src: IteratorSerializer,
                                          property: BeanProperty,
                                          vts: TypeSerializer,
                                          elementSerializer: ValueSerializer[_],
                                          unwrapSingle: jl.Boolean,
                                          suppressableValue: Any,
                                          suppressNulls: Boolean )
  extends AsArraySerializerBase[collection.Iterator[Any]](src, vts, elementSerializer,
    unwrapSingle, property, suppressableValue, suppressNulls)
  with IteratorSerializer {

  override val iteratorSerializer =
    new ScalaIteratorSerializer(src.iteratorSerializer, property, vts, elementSerializer,
      unwrapSingle, suppressableValue, suppressNulls)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new ResolvedIteratorSerializer(src, property, newVts, elementSerializer, unwrapSingle,
      suppressableValue, suppressNulls)
}

private class UnresolvedIteratorSerializer( cls: Class[_],
                                            et: JavaType,
                                            staticTyping: Boolean,
                                            vts: TypeSerializer,
                                            elementSerializer: ValueSerializer[AnyRef] )
  extends AsArraySerializerBase[collection.Iterator[Any]](cls, et, staticTyping, vts, elementSerializer)
  with IteratorSerializer {

  override val iteratorSerializer =
    new ScalaIteratorSerializer(et, staticTyping, vts)

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new UnresolvedIteratorSerializer(cls, et, staticTyping, newVts, elementSerializer)
}

private class ScalaIteratorSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  private val JACKSONSERIALIZABLE_CLASS = classOf[JacksonSerializable]
  private val SCALAITERATOR_CLASS = classOf[collection.Iterator[_]]

  override def findCollectionLikeSerializer(serializationConfig: SerializationConfig,
                                            collectionType: CollectionLikeType,
                                            beanDescription: BeanDescription.Supplier,
                                            formatOverrides: JsonFormat.Value,
                                            elementTypeSerializer: TypeSerializer,
                                            elementSerializer: ValueSerializer[Object]): ValueSerializer[_] = {

    val rawClass = collectionType.getRawClass
    if (!SCALAITERATOR_CLASS.isAssignableFrom(rawClass) || JACKSONSERIALIZABLE_CLASS.isAssignableFrom(rawClass)) None.orNull
    else new UnresolvedIteratorSerializer(rawClass, collectionType.getContentType, false, elementTypeSerializer, elementSerializer)
  }
}

trait IteratorSerializerModule extends IteratorTypeModifierModule {
  override def getModuleName: String = "IteratorSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new ScalaIteratorSerializerResolver(config)
      builder.build()
    }
  }
}

object IteratorSerializerModule extends IteratorSerializerModule
