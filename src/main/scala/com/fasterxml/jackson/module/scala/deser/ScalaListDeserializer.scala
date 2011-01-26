package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import deser.StdDeserializer
import org.codehaus.jackson.{JsonToken,JsonParser}
import java.lang.reflect.Constructor
import scala.collection.mutable.MutableList

class ScalaListDeserializer(collectionType:JavaType, valueDeser:JsonDeserializer[Object], 
		valueTypeDeser:TypeDeserializer, ctor:Constructor[java.util.Collection[Object]])
		extends StdDeserializer[List[_]](classOf[List[_]])
{
	override def deserialize(jp:JsonParser, ctxt:DeserializationContext) = {
		val result = new MutableList[Any]()  // erasure can be our friend
		deserialize(jp, ctxt, result)
		result.toList
    }

	protected def deserialize(jp:JsonParser, ctxt:DeserializationContext, result:MutableList[Any]) {
		// Ok: must point to START_ARRAY
		if (jp.getCurrentToken() != JsonToken.START_ARRAY) 
			throw ctxt.mappingException(collectionType.getRawClass)

		var t:JsonToken = jp.nextToken()

		while (t != JsonToken.END_ARRAY) {
			result += {
				if( t == JsonToken.VALUE_NULL )
					null
				else if( valueTypeDeser == null )
					valueDeser.deserialize(jp,ctxt)
				else
					valueDeser.deserializeWithType(jp,ctxt,valueTypeDeser)
			}
			t = jp.nextToken()
        }
        result
    }
}