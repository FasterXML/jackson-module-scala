package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer};

import com.fasterxml.jackson.databind.`type`.CollectionLikeType;

import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import deser.{ResolvableDeserializer, ContextualDeserializer, Deserializers}

private class OptionDeserializer(elementType: JavaType, var deser: JsonDeserializer[_])
  extends JsonDeserializer[Option[AnyRef]] with ContextualDeserializer {
  
  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[_] = {
    val cd = ctxt.findContextualValueDeserializer(elementType, property)
    if (cd != null) new OptionDeserializer(elementType, cd)
    else this
  }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext) =
    Option(deser.deserialize(jp, ctxt)).asInstanceOf[Option[AnyRef]]

  override def getNullValue = None
}

private object OptionDeserializerResolver extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findCollectionLikeDeserializer(theType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]) =
    if (!OPTION.isAssignableFrom(theType.getRawClass)) null
    else new OptionDeserializer(theType.containedType(0), elementDeserializer)
}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  this += OptionDeserializerResolver
}