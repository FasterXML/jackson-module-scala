package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
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