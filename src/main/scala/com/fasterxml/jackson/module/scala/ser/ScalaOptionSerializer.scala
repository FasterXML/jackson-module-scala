package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._
import ser.{ContainerSerializerBase, CustomSerializerFactory}

class ScalaOptionSerializer extends JsonSerializer[Option[Any]] {
	override def serialize(value:Option[Any], jgen:JsonGenerator, provider:SerializerProvider) {
		jgen.writeStartObject();
		jgen.writeBooleanField("empty",value.isEmpty)
		if( !value.isEmpty ) {
			jgen.writeFieldName("value")
			provider.findValueSerializer(value.get.asInstanceOf[AnyRef].getClass).serialize(value.get.asInstanceOf[AnyRef], jgen, provider)
		}
		jgen.writeEndObject();
	}
}