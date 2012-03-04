package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.{BeanDescription, JavaType, JsonSerializer, SerializationConfig, SerializerProvider};
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

private class OptionSerializer extends JsonSerializer[Option[_]] {

  def serialize(value: Option[_], jgen: JsonGenerator, provider: SerializerProvider) {
    value match {
      case Some(v) => jgen.writeObject(v)
      case None => jgen.writeNull()
    }
  }

}

private object OptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findSerializer(config: SerializationConfig, theType: JavaType, beanDesc: BeanDescription) =
    if (!OPTION.isAssignableFrom(theType.getRawClass)) null
    else new OptionSerializer

}

trait OptionSerializerModule extends OptionTypeModifierModule {
  this += (_ addSerializers OptionSerializerResolver)
}