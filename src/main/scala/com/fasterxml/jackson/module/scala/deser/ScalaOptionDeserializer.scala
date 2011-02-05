package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import org.codehaus.jackson.{JsonToken,JsonParser}
import java.lang.reflect.Constructor
import scala.collection.mutable.MutableList

class ScalaOptionDeserializer[_] extends JsonDeserializer[Option[_]]
{
	override def deserialize(jp:JsonParser, ctxt:DeserializationContext) = {
		/*
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) 
			throw ctxt.mappingException(`type`.getRawClass)
		jp.nextToken
		if (jp.getCurrentToken() != JsonToken.FIELD_NAME) // parse over "empty" property field name to get to its value 
			throw ctxt.mappingException(`type`.getRawClass)
		jp.nextToken
		if( jp.getBooleanValue ) { // 'empty' property
			jp.nextToken // skip END_OBJECT
			None
		} else {
			jp.nextToken // parse over 'value' property name
 			jp.nextToken
			val opt = Some(deser.deserialize(jp,ctxt))
			jp.nextToken
			opt
		}
		*/
		null
    }
}