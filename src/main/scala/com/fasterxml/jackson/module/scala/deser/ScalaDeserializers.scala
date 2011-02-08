package com.fasterxml.jackson.module.scala.deser

import java.lang.Class
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map._
import `type`.{ArrayType, CollectionType, MapType}
import deser.{CustomDeserializerFactory}
import org.codehaus.jackson.`type`.JavaType

/**
 * The ScalaDeserialziers finds implementations of JsonDeserializer that can be used to parse JSON into Scala
 * collections, specifically json to Map and json to Iterable.
 */
class ScalaDeserializers extends CustomDeserializerFactory with Deserializers {


	/**
	 * findBeanDeserializer is called when the ObjectMapper determines the type is not recognized. Since the Scala
	 * collections types do not inherent from the base Java collections API, Jackson will just default to this method.
	 *
	 * This method determines what particular Scala type is being requested for deserialization, then finds its own
	 * Scala-based deserializer.
	 */
	def findBeanDeserializer(javaType: JavaType,
							 config: DeserializationConfig,
							 provider: DeserializerProvider,
							 beanDesc: BeanDescription,
							 property: BeanProperty) = {

		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null;

		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
			// Note: Since Map also implements Iterable, make sure we're checking Map before Iterable.
			deserializer = findMapDeserializer(javaType, config, provider, beanDesc, property)
		} else if (classOf[Iterable[Any]].isAssignableFrom(clazz)) {
			deserializer = findIterableDeserializer(javaType, config, provider, beanDesc, property)
		} else if (classOf[Option[Any]].isAssignableFrom(clazz)) {
			// Todo:
		} else if (classOf[scala.Enumeration$Val].isAssignableFrom(clazz)) {
			// Todo:
		}

		deserializer
	}

	private def findMapDeserializer(javaType: JavaType,
									config: DeserializationConfig,
									provider: DeserializerProvider,
									beanDesc: BeanDescription,
									property: BeanProperty) = {
		val sig = javaType.getGenericSignature
		val keyType = javaType.containedType(0)
		val valueType = javaType.containedType(1)
		val keyDeser = provider.findKeyDeserializer(config, keyType, property)
		val valueDeser = provider.findValueDeserializer(config, valueType, property)
		val valueTypeDeser = null // TODO: will this be a problem? 
		new ScalaMapDeserializer(javaType, keyDeser, valueDeser, valueTypeDeser)
	}

	private def findIterableDeserializer(javaType: JavaType,
										 config: DeserializationConfig,
										 provider: DeserializerProvider,
										 beanDesc: BeanDescription,
										 property: BeanProperty) = {
		val sig = javaType.getGenericSignature
		val contentType = javaType.containedType(0)
		val contentDeser = provider.findValueDeserializer(config, contentType, property)
		val valueTypeDeser = null // TODO: will this be a problem?
		new ScalaListDeserializer(javaType, contentDeser, valueTypeDeser)
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