package com.fasterxml.jackson.module.scala.ser

import scala.collection.Map

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.{BeanDescription, JsonSerializer, SerializationConfig, SerializerProvider};
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers
;
import com.fasterxml.jackson.databind.`type`.MapLikeType;

import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import java.io.StringWriter
;

private class MapSerializer(val keySerializer: Option[JsonSerializer[AnyRef]])
  extends JsonSerializer[Map[_,_]]
{
  override def serialize(value: Map[_, _], jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    value.foreach { case (k,v) => {

      (keySerializer, k) match {
        case (Some(ks), a: AnyRef) => ks.serialize(a, jgen, provider)
        case (_, _) => jgen.writeFieldName(k.toString)
      }

      jgen.writeObject(v)
    }}
    jgen.writeEndObject()
  }

  override def isEmpty(value: Map[_,_]) = value.isEmpty
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
    else new MapSerializer(Option(keySerializer))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}