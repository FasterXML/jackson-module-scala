package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.{BeanDescription, BeanProperty, JavaType, JsonSerializer, Module, SerializationConfig, SerializerProvider};
import com.fasterxml.jackson.databind.jsontype.{TypeSerializer};
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.`type`.CollectionLikeType;

import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

private class OptionSerializer(property: BeanProperty) extends JsonSerializer[Option[AnyRef]] {

  def serialize(value: Option[AnyRef], jgen: JsonGenerator, provider: SerializerProvider)
  {
    if (value.isEmpty) jgen.writeNull()
    else provider.findValueSerializer(value.get.getClass, property).serialize(value.get, jgen, provider)
  }

}

private object OptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findCollectionLikeSerializer(config: SerializationConfig,
          theType: CollectionLikeType, beanDesc: BeanDescription, property: BeanProperty,
          elementTypeSerializer: TypeSerializer, elementValueSerializer: JsonSerializer[Object]): JsonSerializer[_] = {
    if (OPTION.isAssignableFrom(theType.getRawClass)) new OptionSerializer(property)
    else null
  }

}

trait OptionSerializerModule extends OptionTypeModifierModule {
  this += OptionSerializerResolver
}