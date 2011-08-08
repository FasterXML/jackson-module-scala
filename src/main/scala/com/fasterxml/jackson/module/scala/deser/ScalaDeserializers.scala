package com.fasterxml.jackson.module.scala.deser

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._

/**
 * The ScalaDeserialziers finds implementations of JsonDeserializer that can be used to parse JSON into Scala
 * collections, specifically json to Map and json to Iterable.
 */
class ScalaDeserializers extends Deserializers.None {

  override def findBeanDeserializer(javaType: JavaType,
							 config: DeserializationConfig,
							 provider: DeserializerProvider,
							 beanDesc: BeanDescription,
							 property: BeanProperty) = {

		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null;

		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
			deserializer = findMapDeserializer(javaType, config, provider, beanDesc, property)
		} else if (classOf[scala.Enumeration$Value].isAssignableFrom(clazz)) {
			deserializer = new ScalaEnumerationDeserializer(javaType)
		}

		deserializer
	}

	private def findMapDeserializer(javaType: JavaType,
									config: DeserializationConfig,
									provider: DeserializerProvider,
									beanDesc: BeanDescription,
									property: BeanProperty) = {
		val sig = javaType.getGenericSignature
		// Todo: Hacking indicies: parameter, should probably introspect further
		val keyType = javaType.containedType(0)
		val valueType = javaType.containedType(1)
		val keyDeser = provider.findKeyDeserializer(config, keyType, property)
		val valueDeser = provider.findValueDeserializer(config, valueType, property)
		val valueTypeDeser = null // TODO: will this be a problem? 
		new ScalaMapDeserializer(javaType, keyDeser, valueDeser, valueTypeDeser)
	}

}
