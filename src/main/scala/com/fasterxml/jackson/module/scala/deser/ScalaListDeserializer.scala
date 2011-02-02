package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.{CollectionDeserializer, StdDeserializer}
import org.codehaus.jackson.{JsonToken,JsonParser}
import java.lang.reflect.Constructor
import java.util.Collection
import collection.JavaConversions._

class ScalaListDeserializer(val collectionType: JavaType,
		val valueDeser: JsonDeserializer[Object],
		val valueTypeDeser: TypeDeserializer,
		val constructor: Constructor[Collection[Object]]) extends CollectionDeserializer(collectionType, valueDeser, valueTypeDeser, constructor) {

	override def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {
		val collection = super.deserialize(jp, ctxt)
		asIterable(collection)
	}
}