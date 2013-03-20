package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonNode, BeanDescription, JavaType, JsonSerializer, SerializationConfig, SerializerProvider}
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier, Serializers}
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import scala.collection.JavaConverters._
import java.{util => ju}
import com.fasterxml.jackson.databind.`type`.CollectionLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.jsonschema.SchemaAware
import java.lang.reflect.Type
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.node.ObjectNode

private class OptionSerializer(valueSerializer: Option[JsonSerializer[AnyRef]])
  extends StdSerializer[Option[_]](classOf[Option[_]])
  with SchemaAware
{

  def serialize(value: Option[_], jgen: JsonGenerator, provider: SerializerProvider) {
    (value, valueSerializer) match {
      case (Some(v: AnyRef), Some(vs)) => vs.serialize(v, jgen, provider)
      case (Some(v), _) => provider.defaultSerializeValue(v, jgen)
      case (None, _) => provider.defaultSerializeNull(jgen)
    }
  }

  override def isEmpty(value: Option[_]): Boolean = value.isEmpty

  override def getSchema(provider: SerializerProvider, typeHint: Type): JsonNode =
    getSchema(provider, typeHint, isOptional = true)

  override def getSchema(provider: SerializerProvider, typeHint: Type, isOptional: Boolean): JsonNode = {
    val javaType = provider.constructType(typeHint)
    val componentType = javaType.containedType(0)
    val contentSerializer = provider.findValueSerializer(componentType, null)
    val schema = contentSerializer match {
      case cs: SchemaAware => cs.getSchema(provider, componentType.getRawClass, isOptional)
      case _ => null
    }
    val result = if (schema == null) createSchemaNode("any") else schema.asInstanceOf[ObjectNode]
    result.put("required", false)
    result
  }
}

private class OptionPropertyWriter(delegate: BeanPropertyWriter) extends BeanPropertyWriter(delegate)
{
  override def serializeAsField(bean: AnyRef, jgen: JsonGenerator, prov: SerializerProvider) {
    (get(bean), _nullSerializer) match {
      // value is None, which we'll serialize as null, but there's no
      // null-serializer, which means it should be suppressed
      case (None, null) => return
      case _ => super.serializeAsField(bean, jgen, prov)
    }
  }
}

private object OptionBeanSerializerModifier extends BeanSerializerModifier {

  override def changeProperties(config: SerializationConfig,
                                beanDesc: BeanDescription,
                                beanProperties: ju.List[BeanPropertyWriter]): ju.List[BeanPropertyWriter] = {

    beanProperties.asScala.transform { w =>
      if (classOf[Option[_]].isAssignableFrom(w.getPropertyType))
        new OptionPropertyWriter(w)
      else
        w
    }.asJava

  }

}

private object OptionSerializerResolver extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findCollectionLikeSerializer(confg: SerializationConfig,
                                            `type`: CollectionLikeType,
                                            beanDesc: BeanDescription,
                                            elementTypeSerializer: TypeSerializer ,
                                            elementValueSerializer: JsonSerializer[AnyRef]
                                           ): JsonSerializer[_] =

    if (!OPTION.isAssignableFrom(`type`.getRawClass)) null
    else new OptionSerializer(Option(elementValueSerializer))
}



trait OptionSerializerModule extends OptionTypeModifierModule {
  this += { ctx =>
    ctx addSerializers OptionSerializerResolver
    ctx addBeanSerializerModifier OptionBeanSerializerModifier
  }
}
