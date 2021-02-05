package com.fasterxml.jackson
package module.scala
package ser

import java.{lang => jl}

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase
import com.fasterxml.jackson.databind.ser.{ContainerSerializer, Serializers}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.modifiers.IterableTypeModifierModule

private trait IterableSerializer
  extends AsArraySerializerBase[collection.Iterable[Any]]
{
  def collectionSerializer: ScalaIterableSerializer

  override def hasSingleElement(value: collection.Iterable[Any]): Boolean = {
    value.size == 1
  }

  override def serializeContents(value: collection.Iterable[Any], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    collectionSerializer.serializeContents(value, gen, provider)
  }

  override def withResolved(property: BeanProperty,
                            vts: TypeSerializer,
                            elementSerializer: JsonSerializer[_],
                            unwrapSingle: jl.Boolean): ResolvedIterableSerializer =
    new ResolvedIterableSerializer(this, property, vts, elementSerializer, unwrapSingle)

  override def isEmpty(prov: SerializerProvider, value: collection.Iterable[Any]): Boolean = value.isEmpty
}

private class ResolvedIterableSerializer( src: IterableSerializer,
                                          property: BeanProperty,
                                          vts: TypeSerializer,
                                          elementSerializer: JsonSerializer[_],
                                          unwrapSingle: jl.Boolean )
  extends AsArraySerializerBase[collection.Iterable[Any]](src, property, vts, elementSerializer, unwrapSingle)
  with IterableSerializer
{
  val collectionSerializer =
    new ScalaIterableSerializer(src.collectionSerializer, property, vts, elementSerializer, unwrapSingle)

  override def _withValueTypeSerializer(newVts: TypeSerializer): ContainerSerializer[_] =
    new ResolvedIterableSerializer(this, property, newVts, elementSerializer, unwrapSingle)
}


private class UnresolvedIterableSerializer( cls: Class[_],
                                            et: JavaType,
                                            staticTyping: Boolean,
                                            vts: TypeSerializer,
                                            elementSerializer: JsonSerializer[AnyRef] )
  extends AsArraySerializerBase[collection.Iterable[Any]](cls, et, staticTyping, vts, elementSerializer)
  with IterableSerializer
{
  val collectionSerializer =
    new ScalaIterableSerializer(et, staticTyping, vts, elementSerializer)

  override def _withValueTypeSerializer(newVts: TypeSerializer): ContainerSerializer[_] =
    new UnresolvedIterableSerializer(cls, et, staticTyping, newVts, elementSerializer)

}

private object IterableSerializerResolver extends Serializers.Base {

  override def findCollectionLikeSerializer(config: SerializationConfig,
                   collectionType: CollectionLikeType,
                   beanDescription: BeanDescription,
                   elementTypeSerializer: TypeSerializer,
                   elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    val rawClass = collectionType.getRawClass
    if (!classOf[collection.Iterable[Any]].isAssignableFrom(rawClass)) null
    else if (classOf[collection.Map[Any,Any]].isAssignableFrom(rawClass)) null
    else {
      // CollectionSerializer *needs* an elementType, but AsArraySerializerBase *forces*
      // static typing if the element type is final. This makes sense to Java, but Scala
      // corrupts the Java type system in the case of "ValueTypes"; the signature of the
      // collection is marked as the underlying type, but the storage actually holds the
      // value type, causing casts that Jackson does to fail.
      //
      // The workaround is to let Jackson know that it can't rely on the element type
      // by telling it the element type is AnyRef.
      new UnresolvedIterableSerializer(rawClass, config.constructType(classOf[AnyRef]), false, elementTypeSerializer, elementSerializer)
    }
  }

}

trait IterableSerializerModule extends IterableTypeModifierModule {
  this += IterableSerializerResolver
}
