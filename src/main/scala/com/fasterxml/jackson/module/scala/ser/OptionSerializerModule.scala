package com.fasterxml.jackson
package module.scala
package ser

import java.lang.reflect.Type
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsonschema.{JsonSchema, SchemaAware}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.impl.{PropertySerializerMap, UnknownSerializer}
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.ser.{ContextualSerializer, Serializers}
import com.fasterxml.jackson.databind.util.NameTransformer
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import com.fasterxml.jackson.module.scala.util.Implicits._

object OptionSerializer {
  def useStatic(provider: SerializerProvider, property: Option[BeanProperty], referredType: Option[JavaType]): Boolean = {
    if (referredType.isEmpty) return false
    // First: no serializer for `Object.class`, must be dynamic
    if (referredType.get.isJavaLangObject) return false
    // but if type is final, might as well fetch
    if (referredType.get.isFinal) return true
    // also: if indicated by typing, should be considered static
    if (referredType.get.useStaticType()) return true
    // if neither, maybe explicit annotation?
    for (
      ann <- property.flatMap(p => Option(p.getMember));
      intr <- Option(provider.getAnnotationIntrospector)
    ) {
      val typing = intr.findSerializationTyping(ann)
      if (typing == JsonSerialize.Typing.STATIC) return true
      if (typing == JsonSerialize.Typing.DYNAMIC) return false
    }
    // and finally, may be forced by global static typing (unlikely...)
    provider.isEnabled(MapperFeature.USE_STATIC_TYPING)
  }

  def findSerializer(provider: SerializerProvider, typ: Class[_], prop: Option[BeanProperty]): JsonSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    provider.findTypedValueSerializer(typ, true, prop.orNull)
  }

  def findSerializer(provider: SerializerProvider, typ: JavaType, prop: Option[BeanProperty]): JsonSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    provider.findTypedValueSerializer(typ, true, prop.orNull)
  }

  def hasContentTypeAnnotation(provider: SerializerProvider, property: BeanProperty): Boolean = {
    val intr = provider.getAnnotationIntrospector
    if (property == null || intr == null) return false
    intr.refineSerializationType(provider.getConfig, property.getMember, property.getType) != null
  }
}

private class OptionSerializer(referredType: JavaType,
                               property: Option[BeanProperty],
                               valueTypeSerializer: Option[TypeSerializer],
                               valueSerializer: Option[JsonSerializer[AnyRef]],
                               contentInclusion: Option[JsonInclude.Include],
                               unwrapper: Option[NameTransformer],
                               var dynamicSerializers: PropertySerializerMap = PropertySerializerMap.emptyForProperties())
  extends StdSerializer[Option[AnyRef]](referredType)
    with ContextualSerializer
    with SchemaAware {

  import OptionSerializer._

  override def unwrappingSerializer(transformer: NameTransformer): JsonSerializer[Option[AnyRef]] = {
    val ser = valueSerializer.map(_.unwrappingSerializer(transformer))
    val unt = unwrapper.map(NameTransformer.chainedTransformer(transformer, _)).getOrElse(transformer)
    withResolved(property, valueTypeSerializer, ser, Option(unt), contentInclusion)
  }

  protected[this] def withResolved(prop: Option[BeanProperty], vts: Option[TypeSerializer],
                                   valueSer: Option[JsonSerializer[AnyRef]], unt: Option[NameTransformer],
                                   contentIncl: Option[JsonInclude.Include]): OptionSerializer = {
    if (prop == property && vts == valueTypeSerializer && valueSer == valueSerializer &&
      contentIncl == contentInclusion && unt == unwrapper) this
    else new OptionSerializer(referredType, prop, vts, valueSer, contentIncl, unt, dynamicSerializers)
  }

  override def createContextual(prov: SerializerProvider, prop: BeanProperty): JsonSerializer[_] = {
    val propOpt = Option(prop)

    val vts = valueTypeSerializer.optMap(_.forProperty(prop))
    var ser = for (
      prop <- propOpt;
      member <- Option(prop.getMember);
      serDef <- Option(prov.getAnnotationIntrospector.findContentSerializer(member))
    ) yield prov.serializerInstance(member, serDef)
    ser = ser.orElse(valueSerializer).map(prov.handlePrimaryContextualization(_, prop)).asInstanceOf[Option[JsonSerializer[AnyRef]]]
    ser = Option(findConvertingContentSerializer(prov, prop, ser.orNull).asInstanceOf[JsonSerializer[AnyRef]])
    ser = ser match {
      case None => if (hasContentTypeAnnotation(prov, prop)) {
        Option(prov.findValueSerializer(referredType, prop)).filterNot(_.isInstanceOf[UnknownSerializer])
      } else None
      case Some(s) => Option(prov.handlePrimaryContextualization(s, prop).asInstanceOf[JsonSerializer[AnyRef]])
    }

    // A few conditions needed to be able to fetch serializer here:
    if (ser.isEmpty && useStatic(prov, propOpt, Option(referredType))) {
      ser = Option(findSerializer(prov, referredType, propOpt))
    }
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
    withResolved(propOpt, vts, ser, unwrapper, newIncl)
  }

  override def isEmpty(provider: SerializerProvider, value: Option[AnyRef]): Boolean = {
    if (value == null || value.isEmpty) return true
    if (contentInclusion.isEmpty) return false
    val contents = value.get
    valueSerializer
      .getOrElse(findCachedSerializer(provider, contents.getClass))
      .isEmpty(provider, contents)
  }

  override def isUnwrappingSerializer: Boolean = unwrapper.isDefined

  override def serialize(opt: Option[AnyRef], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    if (opt.isEmpty) {
      if (unwrapper.isEmpty) {
        provider.defaultSerializeNull(gen)
      }
      return
    }

    val value = opt.get
    val ser = valueSerializer.getOrElse(findCachedSerializer(provider, value.getClass))
    valueTypeSerializer match {
      case Some(vts) => ser.serializeWithType(value, gen, provider, vts)
      case None => ser.serialize(value, gen, provider)
    }
  }

  override def serializeWithType(opt: Option[AnyRef], gen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
    if (opt.isEmpty) {
      if (unwrapper.isEmpty) {
        provider.defaultSerializeNull(gen)
      }
      return
    }
    // Otherwise apply type-prefix/suffix, then std serialize:
    typeSer.writeTypePrefixForScalar(opt, gen, classOf[Option[_]])
    serialize(opt, gen, provider)
    typeSer.writeTypeSuffixForScalar(opt, gen)
  }

  override def getSchema(provider: SerializerProvider, typeHint: Type): JsonNode =
    getSchema(provider, typeHint, isOptional = true)

  override def getSchema(provider: SerializerProvider, typeHint: Type, isOptional: Boolean): JsonNode = {
    val contentSerializer = valueSerializer.getOrElse {
      val javaType = provider.constructType(typeHint)
      val componentType = javaType.getContentType
      provider.findTypedValueSerializer(componentType, true, property.orNull)
    }
    contentSerializer match {
      case cs: SchemaAware => cs.getSchema(provider, contentSerializer.handledType(), isOptional)
      case _ => JsonSchema.getDefaultSchemaNode
    }
  }

  override def acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, typeHint: JavaType): Unit = {
    var ser = valueSerializer.getOrElse(findSerializer(visitor.getProvider, referredType, property))
    ser = unwrapper.map(ser.unwrappingSerializer).getOrElse(ser)
    ser.acceptJsonFormatVisitor(visitor, referredType)
  }

  protected[this] def findCachedSerializer(prov: SerializerProvider, typ: Class[_]): JsonSerializer[AnyRef] = {
    var ser = dynamicSerializers.serializerFor(typ)
    if (ser == null) {
      ser = findSerializer(prov, typ, property)
      ser = unwrapper.map(ser.unwrappingSerializer).getOrElse(ser)
      dynamicSerializers = dynamicSerializers.newWith(typ, ser)
    }
    ser
  }
}

private object OptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findReferenceSerializer(config: SerializationConfig,
                                       refType: ReferenceType,
                                       beanDesc: BeanDescription,
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) return null
    new OptionSerializer(refType.getReferencedType, property = None,
      valueTypeSerializer = Option(contentTypeSerializer).orElse(Option(refType.getTypeHandler[TypeSerializer])),
      valueSerializer = Option(contentValueSerializer).orElse(Option(refType.getValueHandler[JsonSerializer[AnyRef]])),
      contentInclusion = None, unwrapper = None)
  }
}

trait OptionSerializerModule extends OptionTypeModifierModule {
  this += { ctx =>
    ctx addSerializers OptionSerializerResolver
  }
}
