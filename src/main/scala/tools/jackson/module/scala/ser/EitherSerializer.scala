package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.{JsonFormat, JsonInclude}
import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.ReferenceType
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.impl.{PropertySerializerMap, UnknownSerializer}
import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.modifiers.EitherTypeModifierModule
import tools.jackson.module.scala.util.Implicits._

import scala.language.existentials

private case class EitherDetails(typ: Option[JavaType],
                                 valueTypeSerializer: Option[TypeSerializer],
                                 valueSerializer: Option[ValueSerializer[AnyRef]]) {
  def withHandlers(vtsOpt: Option[TypeSerializer], serOpt: Option[ValueSerializer[AnyRef]]): EitherDetails = {
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
  extends StdSerializer[Either[AnyRef, AnyRef]](classOf[Either[AnyRef, AnyRef]]) {

  import tools.jackson.module.scala.ser.OptionSerializer._

  protected[this] def withResolved(prop: Option[BeanProperty], newLeft: EitherDetails, newRight: EitherDetails,
                                   contentIncl: Option[JsonInclude.Include]): EitherSerializer = {
    if (prop == property && left == newLeft && right == newRight && contentIncl == contentInclusion) this
    else new EitherSerializer(newLeft, newRight, prop, contentIncl, dynamicSerializers)
  }

  protected[this] def createContextualDetails(prov: SerializerProvider,
                                              prop: BeanProperty,
                                              details: EitherDetails): EitherDetails = {
    val vts = details.valueTypeSerializer.optMap(_.forProperty(prov, prop))
    val serializer1 = for (
      prop <- Option(prop);
      member <- Option(prop.getMember);
      serDef <- Option(prov.getAnnotationIntrospector.findContentSerializer(prov.getConfig, member))
    ) yield prov.serializerInstance(member, serDef)
    val serializer2 = serializer1
      .orElse(details.valueSerializer)
      .map(prov.handlePrimaryContextualization(_, prop))
      .asInstanceOf[Option[ValueSerializer[AnyRef]]]
    val serializer3 = Option(findContextualConvertingSerializer(prov, prop, serializer2.orNull))
      .asInstanceOf[Option[ValueSerializer[AnyRef]]]
    var serializerOption = serializer3 match {
      case None => if (details.typ.isDefined && hasContentTypeAnnotation(prov, prop)) {
        Option(prov.findValueSerializer(details.typ.get)).filterNot(_.isInstanceOf[UnknownSerializer])
      } else None
      case Some(s) => Option(prov.handlePrimaryContextualization(s, prop).asInstanceOf[ValueSerializer[AnyRef]])
    }

    // A few conditions needed to be able to fetch serializer here:
    if (serializerOption.isEmpty && useStatic(prov, Option(prop), details.typ)) {
      serializerOption = Option(findSerializer(prov, details.typ.orNull, Option(prop)))
    }

    details.copy(valueTypeSerializer = vts, valueSerializer = serializerOption)
  }

  override def createContextual(prov: SerializerProvider, prop: BeanProperty): ValueSerializer[_] = {
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
    jgen.writeName(field)
    if (content == null) {
      provider.defaultSerializeNullValue(jgen)
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
      provider.defaultSerializeNullValue(jgen)
    } else {
      // Otherwise apply type-prefix/suffix, then std serialize:
      typeSer.writeTypePrefix(jgen, provider, typeSer.typeId(value, JsonToken.START_OBJECT))
      serialize(value, jgen, provider, Some(typeSer))
      typeSer.writeTypeSuffix(jgen, provider, typeSer.typeId(value, JsonToken.END_OBJECT))
    }
  }

  protected[this] def findCachedSerializer(prov: SerializerProvider, typ: Class[_]): ValueSerializer[AnyRef] = {
    var ser = dynamicSerializers.serializerFor(typ).asInstanceOf[ValueSerializer[AnyRef]]
    if (ser == null) {
      ser = findSerializer(prov, typ, property)
      dynamicSerializers = dynamicSerializers.newWith(typ, ser.asInstanceOf[ValueSerializer[Object]])
    }
    ser
  }
}

private class EitherSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  private val EITHER = classOf[Either[AnyRef, AnyRef]]
  private val LEFT = classOf[Left[AnyRef, AnyRef]]
  private val RIGHT = classOf[Right[AnyRef, AnyRef]]

  override def findReferenceSerializer(serializationConfig: SerializationConfig,
                                       refType: ReferenceType,
                                       beanDesc: BeanDescription,
                                       formatOverrides: JsonFormat.Value,
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: ValueSerializer[AnyRef]): ValueSerializer[_] = {
    if (!EITHER.isAssignableFrom(refType.getRawClass)) None.orNull
    else {
      val javaType = if (LEFT.isAssignableFrom(refType.getRawClass) || RIGHT.isAssignableFrom(refType.getRawClass)) {
        refType.getReferencedType.getSuperClass
      } else refType.getReferencedType

      val leftType = javaType.containedType(0)
      val rightType = javaType.containedType(1)

      val typeSer = Option(contentTypeSerializer).orElse(Option(javaType.getTypeHandler.asInstanceOf[TypeSerializer]))
      val valSer = Option(contentValueSerializer).orElse(Option(javaType.getValueHandler.asInstanceOf[ValueSerializer[AnyRef]]))

      val left = EitherDetails(Option(leftType), typeSer, valSer)
      val right = EitherDetails(Option(rightType), typeSer, valSer)

      new EitherSerializer(left.withHandlers(typeSer, valSer), right.withHandlers(typeSer, valSer), None)
    }
  }
}

trait EitherSerializerModule extends EitherTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new EitherSerializerResolver(config)
      builder.build()
    }
  }
}

object EitherSerializerModule extends EitherSerializerModule
