package com.fasterxml.jackson
package module.scala
package ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer
import com.fasterxml.jackson.databind.util.NameTransformer
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

import scala.languageFeature.postfixOps

// This is still here because it is used in other places like EitherSerializer, it is no
// longer used for the Option serializer
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
      val typing = intr.findSerializationTyping(provider.getConfig, ann)
      if (typing == JsonSerialize.Typing.STATIC) return true
      if (typing == JsonSerialize.Typing.DYNAMIC) return false
    }
    // and finally, may be forced by global static typing (unlikely...)
    provider.isEnabled(MapperFeature.USE_STATIC_TYPING)
  }

  def findSerializer(provider: SerializerProvider, typ: Class[_], prop: Option[BeanProperty]): JsonSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    provider.findTypedValueSerializer(typ, true).asInstanceOf[JsonSerializer[AnyRef]]
  }

  def findSerializer(provider: SerializerProvider, typ: JavaType, prop: Option[BeanProperty]): JsonSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    provider.findTypedValueSerializer(typ, true).asInstanceOf[JsonSerializer[AnyRef]]
  }

  def hasContentTypeAnnotation(provider: SerializerProvider, property: BeanProperty): Boolean = {
    val intr = provider.getAnnotationIntrospector
    if (property == null || intr == null) return false
    intr.refineSerializationType(provider.getConfig, property.getMember, property.getType) != null
  }
}

class OptionSerializer(
  refType: ReferenceType,
  staticTyping: Boolean,
  contentTypeSerializer: TypeSerializer,
  contentValueSerializer: JsonSerializer[AnyRef]
) extends ReferenceTypeSerializer[Option[_]](
      refType,
      staticTyping,
      contentTypeSerializer,
      contentValueSerializer
    ) {

  override def withResolved(
    prop: BeanProperty,
    vts: TypeSerializer,
    valueSer: JsonSerializer[_],
    unwrapper: NameTransformer
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      prop,
      vts,
      valueSer,
      unwrapper,
      _suppressableValue,
      _suppressNulls
    )
  }

  override def withContentInclusion(
    suppressableValue: AnyRef,
    suppressNulls: Boolean
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      _property,
      _valueTypeSerializer,
      _valueSerializer,
      _unwrapper,
      suppressableValue,
      suppressNulls
    )
  }

  override protected def _isValuePresent(value: Option[_]): Boolean = value.isDefined

  override protected def _getReferenced(value: Option[_]): AnyRef = value.get.asInstanceOf[AnyRef]

  override protected def _getReferencedIfPresent(value: Option[_]): AnyRef = {
    value.asInstanceOf[Option[AnyRef]].orNull
  }
}

class ResolvedOptionSerializer(
  base: ReferenceTypeSerializer[_],
  property: BeanProperty,
  vts: TypeSerializer,
  valueSer: JsonSerializer[_],
  unwrapper: NameTransformer,
  suppressableValue: AnyRef,
  suppressNulls: Boolean
) extends ReferenceTypeSerializer[Option[_]](
      base,
      property,
      vts,
      valueSer,
      unwrapper,
      suppressableValue,
      suppressNulls
    ) {

  override def withResolved(
    prop: BeanProperty,
    vts: TypeSerializer,
    valueSer: JsonSerializer[_],
    unwrapper: NameTransformer
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      prop,
      vts,
      valueSer,
      unwrapper,
      _suppressableValue,
      _suppressNulls
    )
  }

  override def withContentInclusion(
    suppressableValue: AnyRef,
    suppressNulls: Boolean
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      _property,
      _valueTypeSerializer,
      _valueSerializer,
      _unwrapper,
      suppressableValue,
      suppressNulls
    )
  }

  override protected def _isValuePresent(value: Option[_]): Boolean = value.isDefined

  override protected def _getReferenced(value: Option[_]): AnyRef = value.get.asInstanceOf[AnyRef]

  override protected def _getReferencedIfPresent(value: Option[_]): AnyRef = {
    value.asInstanceOf[Option[AnyRef]].orNull
  }
}

private object OptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findReferenceSerializer(config: SerializationConfig,
                                       refType: ReferenceType,
                                       beanDesc: BeanDescription,
                                       formatOverrides: JsonFormat.Value,
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) return null
    val staticTyping = contentTypeSerializer == null && config.isEnabled(
      MapperFeature.USE_STATIC_TYPING
    )
    new OptionSerializer(refType, staticTyping, contentTypeSerializer, contentValueSerializer)
  }
}

trait OptionSerializerModule extends OptionTypeModifierModule {
  this += { ctx =>
    ctx addSerializers OptionSerializerResolver
  }
}
