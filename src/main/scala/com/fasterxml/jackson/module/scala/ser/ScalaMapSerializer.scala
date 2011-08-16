package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._
import ser.std.ContainerSerializerBase
import collection.Map

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
class ScalaMapSerializer extends ContainerSerializerBase[Map[String,Any]](classOf[Map[String,Any]]) {
	override def serialize(value:Map[String,Any], jgen:JsonGenerator, provider:SerializerProvider) {
		jgen.writeStartObject();
		if( value.size > 0 ) {
			val keySerializer = provider.getKeySerializer
			value.foreach( mapElem => {
				val (k,v) = mapElem
				keySerializer.serialize(k, jgen, provider)
				provider.findValueSerializer(v.asInstanceOf[AnyRef].getClass).serialize(v.asInstanceOf[AnyRef], jgen, provider)
			})
		}
		jgen.writeEndObject();
	}
	override def _withValueTypeSerializer(vts:TypeSerializer) = new ScalaIterableSerializer()
	override def getSchema(provider:SerializerProvider, typeHint:java.lang.reflect.Type) = createSchemaNode("string", true)
}