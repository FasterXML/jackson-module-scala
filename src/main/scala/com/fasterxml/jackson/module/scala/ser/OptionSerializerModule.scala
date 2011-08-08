package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.map.`type`.CollectionLikeType
import java.lang.reflect.Type
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.{JsonSerializer, SerializerProvider, TypeSerializer, BeanProperty, BeanDescription, SerializationConfig, Serializers}
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
  self: JacksonModule =>

  this += OptionSerializerResolver
}