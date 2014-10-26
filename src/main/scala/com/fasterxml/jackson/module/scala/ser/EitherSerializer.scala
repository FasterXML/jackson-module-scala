package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.ser.{ContextualSerializer, Serializers}
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.language.existentials

private class EitherSerializer(elementType: Option[JavaType],
                               valueTypeSerializer: Option[TypeSerializer],
                               beanProperty: Option[BeanProperty],
                               elementSerializer: Option[JsonSerializer[AnyRef]])
  extends StdSerializer[Either[AnyRef, AnyRef]](classOf[Either[AnyRef, AnyRef]])
  with ContextualSerializer {


  def serialize(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider) {
    serializeEither(value, jgen, provider, valueTypeSerializer)
  }

  override def serializeWithType(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer) {
    serializeEither(value, jgen, provider, Option(typeSer))
  }


  private def serializeEither(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider, typeSer: Option[TypeSerializer]) {
    jgen.writeStartObject()
    value.fold(serializeValue(field = "l", _, jgen, provider, typeSer), serializeValue(field = "r", _, jgen, provider, typeSer))
    jgen.writeEndObject()
  }

  private def serializeValue(field: String, forValue: AnyRef, jgen: JsonGenerator, provider: SerializerProvider, typeSer: Option[TypeSerializer]) {
    jgen.writeFieldName(field)
    (Option(forValue), elementSerializer, typeSer) match {
      case (Some(v: AnyRef), Some(vs), _) => vs.serialize(v, jgen, provider)
      case (Some(v), _, Some(ts)) => provider.findValueSerializer(v.getClass, beanProperty.orNull).serializeWithType(v, jgen, provider, ts)
      case (Some(v), _, _) => provider.findValueSerializer(v.getClass, beanProperty.orNull).serialize(v, jgen, provider)
      case (None, _, _) => provider.defaultSerializeNull(jgen)
    }
  }

  def createContextual(prov: SerializerProvider, property: BeanProperty): JsonSerializer[_] = {
    // Based on the version in OptionSerializer
    def serializerFromAnnotation(property: BeanProperty, serializerProvider: SerializerProvider) = {
      Option(property).flatMap(p => Option(p.getMember)).flatMap { m =>
        Option(serializerProvider.getAnnotationIntrospector.findContentSerializer(m))
          .map(serDef => serializerProvider.serializerInstance(m, serDef))
      }
    }
    def hasContentTypeAnnotation(provider: SerializerProvider, property: BeanProperty) =
      Option(property).exists { p =>
        Option(provider.getAnnotationIntrospector.findSerializationContentType(p.getMember, p.getType)).isDefined
      }

    def tryContentSerializer(serializerProvider: SerializerProvider, property: BeanProperty, currentSer: Option[JsonSerializer[_]]) = {
      val ser = Option(findConvertingContentSerializer(serializerProvider, property, currentSer.orNull))
      (ser, elementType) match {
        case (None, Some(et)) if hasContentTypeAnnotation(serializerProvider, property) =>
          Option(serializerProvider.findValueSerializer(et, property))
        case (Some(sr), _) => Option(serializerProvider.handleSecondaryContextualization(sr, property))
        case _ => ser
      }
    }

    val typeSer = valueTypeSerializer.map(_.forProperty(property))
    val candidateSerializer = tryContentSerializer(prov, property, serializerFromAnnotation(property, prov).orElse(elementSerializer))

    if (candidateSerializer != elementSerializer || property != beanProperty.orNull || valueTypeSerializer != typeSer)
      new EitherSerializer(elementType, typeSer, Option(property), candidateSerializer.asInstanceOf[Option[JsonSerializer[AnyRef]]])
    else this
  }

}


private object EitherSerializerResolver extends Serializers.Base {

  private val EITHER = classOf[Either[AnyRef, AnyRef]]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription) =
    if (!EITHER.isAssignableFrom(javaType.getRawClass)) null else {
      val elementType = Option(javaType.containedType(0))
      val typeSer = elementType.flatMap(e => Option(e.getTypeHandler).map(_.asInstanceOf[TypeSerializer]))
      val valSer = elementType.flatMap(e => Option(e.getValueHandler).map(_.asInstanceOf[JsonSerializer[AnyRef]]))

      new EitherSerializer(elementType, typeSer, None, valSer)
    }
}

trait EitherSerializerModule extends JacksonModule {
  this += (_ addSerializers EitherSerializerResolver)
}
