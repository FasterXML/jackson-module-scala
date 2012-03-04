package com.fasterxml.jackson.module.scala.ser

import scala.collection.Map

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.{BeanDescription, JsonSerializer, SerializationConfig, SerializerProvider};
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers
;
import com.fasterxml.jackson.databind.`type`.MapLikeType;

import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule;

private class MapSerializer
  extends JsonSerializer[Map[_,_]]
{
  def serialize(value: Map[_, _], jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    value.foreach { case (k,v) => jgen.writeObjectField(k.toString,v) }
    jgen.writeEndObject()
  }
}

private object MapSerializerResolver extends Serializers.Base {

  val BASE = classOf[collection.Map[_,_]]

  override def findMapLikeSerializer(config: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {


    val rawClass = mapLikeType.getRawClass

    if (!BASE.isAssignableFrom(rawClass)) null
    else new MapSerializer
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}