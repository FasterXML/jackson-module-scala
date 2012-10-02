package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{BeanDescription, JavaType, JsonSerializer, SerializationConfig, SerializerProvider}
import com.fasterxml.jackson.databind.ser.{BeanPropertyWriter, BeanSerializerModifier, Serializers}
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule
import scala.collection.JavaConverters._
import java.{util => ju}

private class OptionSerializer extends JsonSerializer[Option[_]] {

  def serialize(value: Option[_], jgen: JsonGenerator, provider: SerializerProvider) {
    value match {
      case Some(v) => provider.defaultSerializeValue(v, jgen)
      case None => provider.defaultSerializeNull(jgen)
    }
  }

  override def isEmpty(value: Option[_]): Boolean = value.isEmpty
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

  override def findSerializer(config: SerializationConfig, theType: JavaType, beanDesc: BeanDescription) =
    if (!OPTION.isAssignableFrom(theType.getRawClass)) null
    else new OptionSerializer

}



trait OptionSerializerModule extends OptionTypeModifierModule {
  this += { ctx =>
    ctx addSerializers OptionSerializerResolver
    ctx addBeanSerializerModifier OptionBeanSerializerModifier
  }
}
