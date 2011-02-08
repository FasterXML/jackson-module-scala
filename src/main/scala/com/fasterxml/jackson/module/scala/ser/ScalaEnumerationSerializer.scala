package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
class ScalaEnumerationSerializer extends JsonSerializer[scala.Enumeration$Val] {
	override def serialize(value: scala.Enumeration$Val, jgen: JsonGenerator, provider: SerializerProvider) = {
		val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" ).get
		val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
		jgen.writeStartObject();
		jgen.writeStringField("enumClass", enumClass)
		jgen.writeStringField("value", value.toString)
		jgen.writeEndObject();
	}
}