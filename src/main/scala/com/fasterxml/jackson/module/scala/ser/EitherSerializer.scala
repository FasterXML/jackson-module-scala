package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule

private class EitherSerializer extends StdSerializer[Either[_, _]](classOf[Either[_, _]]) {

  def serialize(value: Either[_, _], jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    value.fold(
    { left =>
      jgen.writeFieldName("l")
      provider.defaultSerializeValue(left, jgen)
    },
    { right =>
      jgen.writeFieldName("r")
      provider.defaultSerializeValue(right, jgen)
    } )
    jgen.writeEndObject()
  }
}

private object EitherSerializerResolver extends Serializers.Base {

  private val EITHER = classOf[Either[AnyRef, AnyRef]]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription) =
    if (!EITHER.isAssignableFrom(javaType.getRawClass)) null else new EitherSerializer
}

trait EitherSerializerModule extends JacksonModule {
  this += (_ addSerializers EitherSerializerResolver)
}
