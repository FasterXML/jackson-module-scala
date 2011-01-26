package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.StdDeserializer
import org.codehaus.jackson.{JsonToken,JsonParser}
import java.lang.reflect.Constructor

class ScalaMapDeserializer(collectionType:JavaType, valueDeser:JsonDeserializer[Object], 
		valueTypeDeser:TypeDeserializer, ctor:Constructor[java.util.Collection[Object]])
		extends StdDeserializer[Map[String,_]](classOf[Map[String,_]])
{
	override def deserialize(jp:JsonParser, ctxt:DeserializationContext) = {
		val result = scala.collection.mutable.Map[String,Any]()  // erasure can be our friend
		deserialize(jp, ctxt, result)
		result.toMap
    }

	protected def deserialize(jp:JsonParser, ctxt:DeserializationContext, result:scala.collection.mutable.Map[String,Any]) {
		if (jp.getCurrentToken != JsonToken.START_OBJECT)
			throw ctxt.mappingException(collectionType.getRawClass)

		val typeDeser = valueTypeDeser

        if (jp.getCurrentToken != JsonToken.START_OBJECT ) 
            throw ctxt.mappingException(collectionType.getRawClass)
		while (jp.nextToken != JsonToken.END_OBJECT) {
	        if (jp.getCurrentToken != JsonToken.FIELD_NAME ) 
	            throw ctxt.mappingException(collectionType.getRawClass)
			val mapKey = jp.getText
			jp.nextToken
			result.put(mapKey,valueDeser.deserialize(jp,ctxt))
        }
        result
    }
}