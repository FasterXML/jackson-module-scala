package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.{MapDeserializer}
import org.codehaus.jackson.{JsonParser}
import collection.JavaConversions._
import java.lang.reflect.Constructor
import collection.mutable.{HashMap}

class ScalaMapDeserializer(mapType: JavaType,
						   keyDeser: KeyDeserializer,
						   valueDeser: JsonDeserializer[Object],
						   valueTypeDeser: TypeDeserializer) extends JsonDeserializer[collection.Map[Object, Object]] {

	// TODO: the Java MapDeserializer uses this to build a map instance, but we're hoping to build one ourselves.
	val defaultConstructor: Constructor[java.util.Map[Object, Object]] = null
	val javaMapDeserializer = new MapDeserializer(mapType, defaultConstructor, keyDeser, valueDeser, valueTypeDeser)

	def deserialize(jp: JsonParser, ctxt: DeserializationContext) : collection.Map[Object, Object] = {


		val map = new HashMap[Object, Object]()
		val javaMap = asJavaMap(map)
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