package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import org.codehaus.jackson.map.`type`.CollectionLikeType
import org.codehaus.jackson.map.{DeserializationContext, JsonDeserializer, TypeDeserializer, BeanProperty, BeanDescription, DeserializerProvider, DeserializationConfig, Deserializers}
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.`type`.JavaType

private class OptionDeserializer(elementType: JavaType, config: DeserializationConfig, provider: DeserializerProvider, property: BeanProperty, deser: JsonDeserializer[_])
  extends JsonDeserializer[Option[AnyRef]] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext) =
    Option(deser)
      .orElse(Some(provider.findValueDeserializer(config, elementType, property)))
      .map(_.deserialize(jp, ctxt)).asInstanceOf[Option[AnyRef]]

  override def getNullValue = None
}

private object OptionDeserializerResolver extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findCollectionLikeDeserializer(theType: CollectionLikeType, config: DeserializationConfig,
                                     provider: DeserializerProvider, beanDesc: BeanDescription, property: BeanProperty,
                                     elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) =
    if (!OPTION.isAssignableFrom(theType.getRawClass)) null
    else new OptionDeserializer(theType.containedType(0), config, provider, property, elementDeserializer)

}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  this += OptionDeserializerResolver
}