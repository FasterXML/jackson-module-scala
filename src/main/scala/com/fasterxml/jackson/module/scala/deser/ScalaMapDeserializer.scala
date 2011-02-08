package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.{MapDeserializer}
import org.codehaus.jackson.{JsonParser}
import collection.JavaConversions._
import java.lang.reflect.Constructor
import collection.mutable.{HashMap}

/**
 * ScalaMapDeserializer deserializes json maps into Scala Maps.
 *
 * Note: See ScalaListDeserializer for implementation details. I've made some assumptions about the implementation
 * of the Java deserializers, simply wrapping them with my own implementations, wrapping them with scala based
 * collections.
 */
class ScalaMapDeserializer(mapType: JavaType,
						   keyDeser: KeyDeserializer,
						   valueDeser: JsonDeserializer[Object],
						   valueTypeDeser: TypeDeserializer) extends JsonDeserializer[collection.Map[Object, Object]] {

	val defaultConstructor: Constructor[java.util.Map[Object, Object]] = null
	val javaMapDeserializer = new MapDeserializer(mapType, defaultConstructor, keyDeser, valueDeser, valueTypeDeser)

	def deserialize(jp: JsonParser, ctxt: DeserializationContext) : collection.Map[Object, Object] = {
		// Again, see ScalaListDeserializer for more info about this implementation.

		val map = new HashMap[Object, Object]()
		val javaMap = asJavaMap(map)

		// Let the Java deserializer do the real work here - the actual parsing - and make calls to our thin Map facade.
		javaMapDeserializer.deserialize(jp, ctxt, javaMap)

		if (isImmutableTypeRequested) {
			map.toMap
		} else {
			map
		}
	}

	private def isImmutableTypeRequested = {
		classOf[collection.immutable.Map[String, Any]].isAssignableFrom(mapType.getRawClass)
	}
}