package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
;
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer};

import com.fasterxml.jackson.databind.`type`.CollectionLikeType;

import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import deser.{ResolvableDeserializer, ContextualDeserializer, Deserializers}

private class OptionDeserializer(elementType: JavaType,
                                 valueTypeDeser: Option[TypeDeserializer],
                                 beanProperty: Option[BeanProperty],
                                 elementDeser: Option[JsonDeserializer[_]])
  extends StdDeserializer[Option[AnyRef]](classOf[Option[AnyRef]]) with ContextualDeserializer {
  
  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
    val typeDeser = valueTypeDeser.map(_.forProperty(property))
    val deser: Option[JsonDeserializer[_]] =
      (for {
        p <- Option(property)
        m <- Option(p.getMember)
        deserDef <- Option(ctxt.getAnnotationIntrospector.findContentDeserializer(m))
      } yield ctxt.deserializerInstance(m, deserDef)).orElse(elementDeser)
    val deser1: Option[JsonDeserializer[_]] = Option(findConvertingContentDeserializer(ctxt, property, deser.orNull))
    val deser2: Option[JsonDeserializer[_]] = if (deser1.isEmpty) {
      if (hasContentTypeAnnotation(ctxt, property)) {
        Option(ctxt.findContextualValueDeserializer(elementType, property))
      } else {
        deser1
      }
    } else {
      Option(ctxt.handleSecondaryContextualization(deser1.get, property))
    }
    if (deser2 != elementDeser || property != beanProperty.orNull || valueTypeDeser != typeDeser)
      new OptionDeserializer(elementType, typeDeser, Option(property), deser2.asInstanceOf[Option[JsonDeserializer[AnyRef]]])
    else this
  }

  def hasContentTypeAnnotation(ctxt: DeserializationContext, property: BeanProperty) = (for {
    p <- Option(property)
    intr <- Option(ctxt.getAnnotationIntrospector)
  } yield {
    intr.findDeserializationContentType(p.getMember, p.getType)
  }).isDefined

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext) = valueTypeDeser match {
    case Some(d) => deserializeWithType(jp, ctxt, d)
    case None => Option {
      elementDeser.map(_.deserialize(jp, ctxt)).getOrElse {
        ctxt.findContextualValueDeserializer(elementType, beanProperty.orNull).deserialize(jp, ctxt)
      }
    }.asInstanceOf[Option[AnyRef]]
  }

  override def deserializeWithType(jp: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer) = Option {
    elementDeser.map(_.deserializeWithType(jp, ctxt, typeDeserializer)).getOrElse {
      ctxt.findContextualValueDeserializer(elementType, beanProperty.orNull).deserializeWithType(jp, ctxt, typeDeserializer)
    }
  }

  override def getNullValue = None
}

private object OptionDeserializerResolver extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findCollectionLikeDeserializer(theType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementValueDeserializer: JsonDeserializer[_]) =
    if (!OPTION.isAssignableFrom(theType.getRawClass)) null
    else {
      val elementType = theType.containedType(0)
      val typeDeser = Option(elementTypeDeserializer).orElse(Option(elementType.getTypeHandler.asInstanceOf[TypeDeserializer]))
      val valDeser: Option[JsonDeserializer[_]] = Option(elementValueDeserializer).orElse(Option(elementType.getValueHandler))
      new OptionDeserializer(elementType, typeDeser, None, valDeser)
    }
}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  this += OptionDeserializerResolver
}