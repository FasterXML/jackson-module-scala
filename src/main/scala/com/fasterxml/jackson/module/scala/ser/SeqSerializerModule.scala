package com.fasterxml.jackson.module.scala.ser

import collection.JavaConverters._
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.modifiers.SeqTypeModifierModule
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.{BeanDescription, SerializationConfig, Serializers, SerializerProvider, JsonSerializer, BeanProperty, TypeSerializer}
import org.codehaus.jackson.map.`type`.CollectionLikeType
import org.codehaus.jackson.map.ser.std.{CollectionSerializer, AsArraySerializerBase}

private class SeqSerializer(seqType: Class[_], elemType: JavaType, staticTyping: Boolean, vts: Option[TypeSerializer], property: BeanProperty, valueSerializer: Option[JsonSerializer[AnyRef]])
  extends AsArraySerializerBase[collection.Seq[Any]](seqType, elemType, staticTyping, vts.orNull, property, valueSerializer.orNull) {

  val collectionSerializer =
    new CollectionSerializer(elemType, staticTyping, vts.orNull, property, valueSerializer.orNull)

  def serializeContents(value: Seq[Any], jgen: JsonGenerator, provider: SerializerProvider)
  {
    collectionSerializer.serializeContents(value.asJavaCollection, jgen, provider)
  }

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new SeqSerializer(seqType, elemType, staticTyping, Option(newVts), property, valueSerializer)
}

private object SeqSerializerResolver extends Serializers.None {

  override def findCollectionLikeSerializer(config: SerializationConfig,
                   collectionType: CollectionLikeType,
                   beanDescription: BeanDescription,
                   beanProperty: BeanProperty,
                   elementTypeSerializer: TypeSerializer,
                   elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    val rawClass = collectionType.getRawClass
    if (!classOf[collection.Seq[Any]].isAssignableFrom(rawClass)) null else
    new SeqSerializer(rawClass, collectionType.containedType(0), false, Option(elementTypeSerializer), beanProperty,
      Option(elementSerializer))
  }

}

trait SeqSerializerModule extends SeqTypeModifierModule {
  self: JacksonModule =>

  this += SeqSerializerResolver
}