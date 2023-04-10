package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.{ReferenceType, TypeFactory}
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

private class OptionDeserializer(fullType: JavaType,
                                 valueTypeDeserializer: Option[TypeDeserializer],
                                 valueDeserializer: Option[JsonDeserializer[AnyRef]],
                                 beanProperty: Option[BeanProperty] = None)
  extends ReferenceTypeDeserializer[Option[AnyRef]](fullType, None.orNull, valueTypeDeserializer.orNull, valueDeserializer.orNull) with ContextualDeserializer {

  override def getValueType: JavaType = fullType

  override def getNullValue: Option[AnyRef] = None
  override def getNullValue(ctxt: DeserializationContext): Option[AnyRef] = None

  private[this] def withResolved(fullType: JavaType,
                                 typeDeser: Option[TypeDeserializer],
                                 valueDeser: Option[JsonDeserializer[_]],
                                 beanProperty: Option[BeanProperty]): OptionDeserializer = {
    if (fullType == this.fullType &&
      typeDeser == this.valueTypeDeserializer &&
      valueDeser == this.valueDeserializer &&
      beanProperty == this.beanProperty) {
      this
    } else {
      new OptionDeserializer(fullType, typeDeser, valueDeser.asInstanceOf[Option[JsonDeserializer[AnyRef]]], beanProperty)
    }
  }

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[Option[AnyRef]] = {
    val typeDeser = valueTypeDeserializer.map(_.forProperty(property))
    var deser = valueDeserializer
    var typ = fullType

    def refdType() = Option(typ.getContentType).getOrElse(TypeFactory.unknownType())

    if (deser.isEmpty) {
      if (property != null) {
        val intr = ctxt.getAnnotationIntrospector
        val member = property.getMember
        if (intr != null && member != null) {
          typ = intr.refineDeserializationType(ctxt.getConfig, member, typ)
        }
        deser = Option(ctxt.findContextualValueDeserializer(refdType(), property))
      }
    } else { // otherwise directly assigned, probably not contextual yet:
      deser = Option(ctxt.handleSecondaryContextualization(deser.get, property, refdType()).asInstanceOf[JsonDeserializer[AnyRef]])
    }

    withResolved(typ, typeDeser, deser, Option(property))
  }

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Option[AnyRef] = {
    val deser = valueDeserializer.getOrElse(ctxt.findContextualValueDeserializer(fullType.getContentType, beanProperty.orNull))
    val refd: AnyRef = valueTypeDeserializer match {
      case Some(vtd) => deser.deserializeWithType(p, ctxt, vtd)
      case None if p.currentToken() == JsonToken.VALUE_NULL => null
      case _ => deser.deserialize(p, ctxt)
    }
    Option(refd)
  }

  override def deserializeWithType(jp: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer): Option[AnyRef] = {
    val t = jp.getCurrentToken
    if (t == JsonToken.VALUE_NULL) {
      getNullValue(ctxt)
    } else {
      valueTypeDeserializer match {
        case Some(vtd) => Option(vtd.deserializeTypedFromAny(jp, ctxt))
        case _ => {
          typeDeserializer.deserializeTypedFromAny(jp, ctxt) match {
            case Some(any) => referenceValue(any)
            case any => referenceValue(any)
          }
        }
      }
    }
  }

  override def referenceValue(contents: Any): Option[AnyRef] = {
    Option(contents) match {
      case Some(anyRef: AnyRef) => Some(anyRef)
      case _ => None
    }
  }

  override def withResolved(typeDeser: TypeDeserializer, valueDeser: JsonDeserializer[_]): ReferenceTypeDeserializer[Option[AnyRef]] = {
    new OptionDeserializer(fullType, Option(typeDeser), Option(valueDeser).map(_.asInstanceOf[JsonDeserializer[AnyRef]]), beanProperty)
  }

  override def updateReference(reference: Option[AnyRef], contents: Any): Option[AnyRef] = referenceValue(contents)

  override def getReferenced(reference: Option[AnyRef]): AnyRef = reference.orNull
}

private object OptionDeserializerResolver extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findReferenceDeserializer(refType: ReferenceType,
                                         config: DeserializationConfig,
                                         beanDesc: BeanDescription,
                                         contentTypeDeserializer: TypeDeserializer,
                                         contentDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) None.orNull
    else {
      val elementType = refType.getContentType
      val typeDeser = Option(contentTypeDeserializer).orElse(Option(elementType.getTypeHandler[TypeDeserializer]))
      val valDeser = Option(contentDeserializer).orElse(Option(elementType.getValueHandler)).asInstanceOf[Option[JsonDeserializer[AnyRef]]]
      new OptionDeserializer(refType, typeDeser, valDeser)
    }
  }
}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  override def getModuleName: String = "OptionDeserializerModule"
  this += OptionDeserializerResolver
}
