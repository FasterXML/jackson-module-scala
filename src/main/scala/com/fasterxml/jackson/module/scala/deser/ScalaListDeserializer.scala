package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.{CollectionDeserializer, StdDeserializer}
import java.lang.reflect.Constructor
import java.util.Collection
import collection.JavaConversions._
import collection.mutable.ListBuffer
import org.codehaus.jackson._

class ScalaListDeserializer(val collectionType: JavaType,
		val valueDeser: JsonDeserializer[Object],
		val valueTypeDeser: TypeDeserializer,
		val constructor: Constructor[Collection[Object]]) extends JsonDeserializer[ListBuffer[Any]] {


	override def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {

		val list = new ListBuffer[Any]()
		// Ok: must point to START_ARRAY (or equivalent)
		if (!jp.isExpectedStartArrayToken()) {
			throw ctxt.mappingException(classOf[ListBuffer[Any]]);
		}

		var t = jp.nextToken()
		while (t != JsonToken.END_ARRAY) {
			var value : Object = null

			if (t == JsonToken.VALUE_NULL) {
				value = null;
			} else {
				value = valueDeser.deserialize(jp, ctxt);
			}
			list += value
			t = jp.nextToken()
		}

		list
	}
}