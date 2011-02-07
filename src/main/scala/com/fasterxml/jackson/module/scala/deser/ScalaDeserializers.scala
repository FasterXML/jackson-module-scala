package com.fasterxml.jackson.module.scala.deser

import java.lang.Class
import org.codehaus.jackson.JsonNode
import com.fasterxml.jackson.module.scala.ser.ScalaMapSerializer
import org.codehaus.jackson.map._
import `type`.{ArrayType, CollectionType, MapType}
import deser.{CustomDeserializerFactory}
import org.codehaus.jackson.`type`.JavaType

/**
 */

class ScalaDeserializers extends CustomDeserializerFactory with Deserializers {

		/*
		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
		} else if (classOf[Iterable[Any]].isAssignableFrom(clazz)) {
		} else if (classOf[Option[Any]].isAssignableFrom(clazz)) {
		} else if (classOf[scala.Enumeration$Val].isAssignableFrom(clazz)) {
		} else {
		}
		*/

	def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, provider: DeserializerProvider, beanDesc: BeanDescription, property: BeanProperty) = {
		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null;

		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
			val sig = javaType.getGenericSignature
			val keyType = javaType.containedType(0)
			val valueType = javaType.containedType(1)
			val keyDeser = provider.findKeyDeserializer(config, keyType, property)
			val valueDeser = provider.findValueDeserializer(config, valueType, property)
			val valueTypeDeser = null // TODO: will this be a problem?
			deserializer = new ScalaMapDeserializer(javaType, keyDeser, valueDeser, valueTypeDeser)
		} else if (classOf[Iterable[Any]].isAssignableFrom(clazz)) {
			val sig = javaType.getGenericSignature
			val contentType = javaType.containedType(0)
			val contentDeser = provider.findValueDeserializer(config, contentType, property)
			val valueTypeDeser = null
			deserializer = new ScalaListDeserializer(javaType, contentDeser, valueTypeDeser)
		} else if (classOf[Option[Any]].isAssignableFrom(clazz)) {
		} else if (classOf[scala.Enumeration$Val].isAssignableFrom(clazz)) {
		}

		deserializer
	}

	def findTreeNodeDeserializer(nodeType: Class[_ <: JsonNode], config: DeserializationConfig, property: BeanProperty) = {
		null
	}

	def findMapDeserializer(mapType: MapType, config: DeserializationConfig, provider: DeserializerProvider, beanDesc: BeanDescription, property: BeanProperty, keyDeserializer: KeyDeserializer, elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) = {
		null
	}

	def findEnumDeserializer(clazz: Class[_], config: DeserializationConfig, beanDesc: BeanDescription, property: BeanProperty) = {
		null
	}

	def findCollectionDeserializer(collectionType: CollectionType, config: DeserializationConfig, provider: DeserializerProvider, beanDesc: BeanDescription, property: BeanProperty, elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) = {
		null
	}

	def findArrayDeserializer(arrayType: ArrayType, config: DeserializationConfig, provider: DeserializerProvider, property: BeanProperty,
							  elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) = {
		null
	}
}