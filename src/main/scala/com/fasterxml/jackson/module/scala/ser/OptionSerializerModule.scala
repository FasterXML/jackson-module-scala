package com.fasterxml.jackson
package module.scala
package ser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.ReferenceType
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer
import com.fasterxml.jackson.databind.util.NameTransformer
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

import scala.util.control.Breaks.{break, breakable}

// This is still here because it is used in other places like EitherSerializer, it is no
// longer used for the Option serializer
object OptionSerializer {
  def useStatic(provider: SerializerProvider, property: Option[BeanProperty], referredType: Option[JavaType]): Boolean = {
    if (referredType.isEmpty) false
    // First: no serializer for `Object.class`, must be dynamic
    else if (referredType.get.isJavaLangObject) false
    // but if type is final, might as well fetch
    else if (referredType.get.isFinal) true
    // also: if indicated by typing, should be considered static
    else if (referredType.get.useStaticType()) true
    // if neither, maybe explicit annotation?
    else {
      var result: Option[Boolean] = None
      breakable {
        for (
          ann <- property.flatMap(p => Option(p.getMember));
          intr <- Option(provider.getAnnotationIntrospector)
        ) {
          val typing = intr.findSerializationTyping(ann)
          if (typing == JsonSerialize.Typing.STATIC) {
            result = Some(true)
            break()
          }
          if (typing == JsonSerialize.Typing.DYNAMIC) {
            result = Some(false)
            break()
          }
        }
      }
      result match {
        case Some(bool) => bool
        case _ =>
          // and finally, may be forced by global static typing (unlikely...)
          provider.isEnabled(MapperFeature.USE_STATIC_TYPING)
      }
    }
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
    if (property == null || intr == null) {
      false
    } else {
      intr.refineSerializationType(provider.getConfig, property.getMember, property.getType) != null
    }
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
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) None.orNull
    else {
      val staticTyping = contentTypeSerializer == null && config.isEnabled(
        MapperFeature.USE_STATIC_TYPING
      )
      new OptionSerializer(refType, staticTyping, contentTypeSerializer, contentValueSerializer)
    }
  }
}

trait OptionSerializerModule extends OptionTypeModifierModule {
  override def getModuleName: String = "OptionSerializerModule"
  this += { ctx =>
    ctx addSerializers OptionSerializerResolver
  }
}
