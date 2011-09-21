package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson.`type`.JavaType
import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.JsonGenerator

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
private class EnumerationSerializer extends JsonSerializer[scala.Enumeration$Val] {
	override def serialize(value: scala.Enumeration$Val, jgen: JsonGenerator, provider: SerializerProvider) = {
		val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
		val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
		jgen.writeStartObject();
		jgen.writeStringField("enumClass", enumClass)
		jgen.writeStringField("value", value.toString)
		jgen.writeEndObject();
	}
}

private object EnumerationSerializerResolver extends Serializers.Base {

  override def findSerializer(config: SerializationConfig,
					   javaType: JavaType,
					   beanDescription: BeanDescription,
					   beanProperty: BeanProperty): JsonSerializer[_] = {
		val clazz = javaType.getRawClass

    if (classOf[scala.Enumeration$Val].isAssignableFrom(clazz)) {
        new EnumerationSerializer
    } else {
      null
    }
	}

}

trait EnumerationSerializerModule extends JacksonModule {
  this += EnumerationSerializerResolver
}