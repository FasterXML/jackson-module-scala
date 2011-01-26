package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson._
import ser.{ContainerSerializerBase, CustomSerializerFactory}

class ScalaIterableSerializer extends ContainerSerializerBase[Iterable[Any]](classOf[Iterable[Any]]) {
	override def serialize(value:Iterable[Any], jgen:JsonGenerator, provider:SerializerProvider) {
		jgen.writeStartArray();
		if( value.size > 0 ) {
			value.foreach( x =>
				provider.findValueSerializer(x.asInstanceOf[AnyRef].getClass).serialize(x.asInstanceOf[AnyRef], jgen, provider)
			)
		}
		jgen.writeEndArray();
	}
	override def _withValueTypeSerializer(vts:TypeSerializer) = new ScalaIterableSerializer()
	override def getSchema(provider:SerializerProvider, typeHint:java.lang.reflect.Type) = createSchemaNode("string", true)
}