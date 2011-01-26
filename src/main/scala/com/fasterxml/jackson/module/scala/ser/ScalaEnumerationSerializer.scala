package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._
import ser.{ContainerSerializerBase, CustomSerializerFactory}

class ScalaEnumerationSerializer extends JsonSerializer[Enumeration] {
	override def serialize(value: Enumeration, jgen: JsonGenerator, provider: SerializerProvider) = {
		val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
		val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
		jgen.writeStartObject();
		jgen.writeStringField("enumClass", enumClass)
		jgen.writeStringField("value", value.toString)
		jgen.writeEndObject();
	}
}