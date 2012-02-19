package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer};
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers};
import com.fasterxml.jackson.databind.`type`.CollectionLikeType;

import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

private class OptionDeserializer(elementType: JavaType, config: DeserializationConfig, deser: JsonDeserializer[_])
  extends JsonDeserializer[Option[AnyRef]] with ContextualDeserializer {
  // !!! TODO 18-Feb-2012, tatu: Must do this in 'createContextual()'
  def deserialize(jp: JsonParser, ctxt: DeserializationContext) =
    Option(deser)
      .orElse(Some(provider.findValueDeserializer(config, elementType, property)))
      .map(_.deserialize(jp, ctxt)).asInstanceOf[Option[AnyRef]]

  override def getNullValue = None
}

private object OptionDeserializerResolver extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findCollectionLikeDeserializer(theType: CollectionLikeType, config: DeserializationConfig,
                                     beanDesc: BeanDescription,
                                     elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) =
    if (OPTION.isAssignableFrom(theType.getRawClass)) new OptionDeserializer(theType.containedType(0), config, elementDeserializer)
    else null
}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  this += OptionDeserializerResolver
}