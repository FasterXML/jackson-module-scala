package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.{JsonGenerator, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.impl.{PropertySerializerMap, UnknownSerializer}
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.ser.{ContextualSerializer, Serializers}
import com.fasterxml.jackson.module.scala.modifiers.EitherTypeModifierModule
import com.fasterxml.jackson.module.scala.util.Implicits._

import scala.language.existentials

private case class EitherDetails(typ: Option[JavaType],
                                 valueTypeSerializer: Option[TypeSerializer],
                                 valueSerializer: Option[JsonSerializer[AnyRef]]) {
  def withHandlers(vtsOpt: Option[TypeSerializer], serOpt: Option[JsonSerializer[AnyRef]]): EitherDetails = {
    var newType = typ
    for (vts <- vtsOpt) {
      newType = newType.map(_.withTypeHandler(vts))
    }
    for (ser <- serOpt) {
      newType = newType.map(_.withValueHandler(ser))
    }
    copy(typ = newType)
  }
}

private class EitherSerializer(left: EitherDetails,
                               right: EitherDetails,
                               property: Option[BeanProperty],
                               contentInclusion: Option[JsonInclude.Include] = None,
                               var dynamicSerializers: PropertySerializerMap = PropertySerializerMap.emptyForProperties())
  extends StdSerializer[Either[AnyRef, AnyRef]](classOf[Either[AnyRef, AnyRef]])
    with ContextualSerializer {

  import com.fasterxml.jackson.module.scala.ser.OptionSerializer._

  protected[this] def withResolved(prop: Option[BeanProperty], newLeft: EitherDetails, newRight: EitherDetails,
                                   contentIncl: Option[JsonInclude.Include]): EitherSerializer = {
    if (prop == property && left == newLeft && right == newRight && contentIncl == contentInclusion) this
    else new EitherSerializer(newLeft, newRight, prop, contentIncl, dynamicSerializers)
  }

  protected[this] def createContextualDetails(prov: SerializerProvider,
                                              prop: BeanProperty,
                                              details: EitherDetails): EitherDetails = {
    val vts = details.valueTypeSerializer.optMap(_.forProperty(prop))
    var ser = for (
      prop <- Option(prop);
      member <- Option(prop.getMember);
      serDef <- Option(prov.getAnnotationIntrospector.findContentSerializer(member))
    ) yield prov.serializerInstance(member, serDef)
    ser = ser
      .orElse(details.valueSerializer)
      .map(prov.handlePrimaryContextualization(_, prop))
      .asInstanceOf[Option[JsonSerializer[AnyRef]]]
    ser = Option(findContextualConvertingSerializer(prov, prop, ser.orNull))
      .asInstanceOf[Option[JsonSerializer[AnyRef]]]
    ser = ser match {
      case None => if (details.typ.isDefined && hasContentTypeAnnotation(prov, prop)) {
        Option(prov.findValueSerializer(details.typ.get, prop)).filterNot(_.isInstanceOf[UnknownSerializer])
      } else None
      case Some(s) => Option(prov.handlePrimaryContextualization(s, prop).asInstanceOf[JsonSerializer[AnyRef]])
    }

    // A few conditions needed to be able to fetch serializer here:
    if (ser.isEmpty && useStatic(prov, Option(prop), details.typ)) {
      ser = Option(findSerializer(prov, details.typ.orNull, Option(prop)))
    }

    details.copy(valueTypeSerializer = vts, valueSerializer = ser)
  }

  override def createContextual(prov: SerializerProvider, prop: BeanProperty): JsonSerializer[_] = {
    val propOpt = Option(prop)

    val newLeft = createContextualDetails(prov, prop, left)
    val newRight = createContextualDetails(prov, prop, right)

    // Also: may want to have more refined exclusion based on referenced value
    val newIncl = propOpt match {
      case None => contentInclusion
      case Some(p) =>
        val pinc = p.findPropertyInclusion(prov.getConfig, classOf[Option[AnyRef]])
        val incl = pinc.getContentInclusion
        if (incl != JsonInclude.Include.USE_DEFAULTS) {
          Some(incl)
        } else contentInclusion
    }
    withResolved(propOpt, newLeft, newRight, newIncl)
  }

  override def serialize(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    serialize(value, jgen, provider, None)
  }

  def serialize(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider, vts: Option[TypeSerializer]): Unit = {
    val (field, content, details) = value match {
      case Left(c) => ("l", c, left)
      case Right(c) => ("r", c, right)
    }
    jgen.writeStartObject()
    jgen.writeFieldName(field)
    if (content == null) {
      provider.defaultSerializeNull(jgen)
    } else {
      val ser = details.valueSerializer.getOrElse(findCachedSerializer(provider, content.getClass))
      vts.orElse(details.valueTypeSerializer) match {
        case Some(vts) => ser.serializeWithType(content, jgen, provider, vts)
        case None => ser.serialize(content, jgen, provider)
      }
    }
    jgen.writeEndObject()
  }

  override def serializeWithType(value: Either[AnyRef, AnyRef], jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
    if (value == null) {
      provider.defaultSerializeNull(jgen)
    } else {
      // Otherwise apply type-prefix/suffix, then std serialize:
      typeSer.writeTypePrefix(jgen, typeSer.typeId(value, JsonToken.START_OBJECT))
      serialize(value, jgen, provider, Some(typeSer))
      typeSer.writeTypeSuffix(jgen, typeSer.typeId(value, JsonToken.END_OBJECT))
    }
  }

  protected[this] def findCachedSerializer(prov: SerializerProvider, typ: Class[_]): JsonSerializer[AnyRef] = {
    var ser = dynamicSerializers.serializerFor(typ)
    if (ser == null) {
      ser = findSerializer(prov, typ, property)
      dynamicSerializers = dynamicSerializers.newWith(typ, ser)
    }
    ser
  }
}

private object EitherSerializerResolver extends Serializers.Base {

  private val EITHER = classOf[Either[AnyRef, AnyRef]]
  private val LEFT = classOf[Left[AnyRef, AnyRef]]
  private val RIGHT = classOf[Right[AnyRef, AnyRef]]

  override def findReferenceSerializer(config: SerializationConfig,
                                       refType: ReferenceType,
                                       beanDesc: BeanDescription,
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {
    if (!EITHER.isAssignableFrom(refType.getRawClass)) None.orNull
    else {
      val javaType = if (LEFT.isAssignableFrom(refType.getRawClass) || RIGHT.isAssignableFrom(refType.getRawClass)) {
        refType.getReferencedType.getSuperClass
      } else refType.getReferencedType

      val leftType = javaType.containedType(0)
      val rightType = javaType.containedType(1)

      val typeSer = Option(contentTypeSerializer).orElse(Option(javaType.getTypeHandler[TypeSerializer]))
      val valSer = Option(contentValueSerializer).orElse(Option(javaType.getValueHandler[JsonSerializer[AnyRef]]))

      val left = EitherDetails(Option(leftType), typeSer, valSer)
      val right = EitherDetails(Option(rightType), typeSer, valSer)

      new EitherSerializer(left.withHandlers(typeSer, valSer), right.withHandlers(typeSer, valSer), None)
    }
  }
}

trait EitherSerializerModule extends EitherTypeModifierModule {
  this += (_ addSerializers EitherSerializerResolver)
}
