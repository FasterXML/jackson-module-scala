package com.fasterxml.jackson.module.scala.deser

import java.lang.Class
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.`type`.{ArrayType, CollectionType, CollectionLikeType, MapType, MapLikeType}
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._

/**
 * The ScalaDeserialziers finds implementations of JsonDeserializer that can be used to parse JSON into Scala
 * collections, specifically json to Map and json to Iterable.
 */
class ScalaDeserializers extends Deserializers {

	def findArrayDeserializer(arrayType: ArrayType,
							  config: DeserializationConfig,
							  provider: DeserializerProvider,
							  property: BeanProperty,
							  elementTypeDeserializer: TypeDeserializer,
							  elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
		null
	}

	def findCollectionDeserializer(collectionType: CollectionType,
								   config: DeserializationConfig,
								   provider: DeserializerProvider,
								   beanDesc: BeanDescription,
								   property: BeanProperty,
								   elementTypeDeserializer: TypeDeserializer,
								   elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
		null
	}

	def findCollectionLikeDeserializer(collectionType: CollectionLikeType,
									   config: DeserializationConfig,
									   provider: DeserializerProvider,
									   beanDesc: BeanDescription,
									   property: BeanProperty,
									   elementTypeDeserializer: TypeDeserializer,
									   elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val resolvedDeserializer =
      Option(elementDeserializer).getOrElse(provider.findValueDeserializer(config,collectionType.containedType(0),property))
    new SeqDeserializer(collectionType, config, resolvedDeserializer, elementTypeDeserializer)
  }

	def findEnumDeserializer(beanType: Class[_],
							 config: DeserializationConfig,
							 beanDesc: BeanDescription,
							 property: BeanProperty): JsonDeserializer[_] = {
		null
	}

	def findMapDeserializer(mapType: MapType,
							config: DeserializationConfig,
							provider: DeserializerProvider,
							beanDesc: BeanDescription,
							property: BeanProperty,
							keyDeserializer: KeyDeserializer,
							elementTypeDeserializer: TypeDeserializer,
							elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
		null
	}

	def findMapLikeDeserializer(mapLikeType: MapLikeType,
								config: DeserializationConfig,
								provider: DeserializerProvider,
								beanDesc: BeanDescription,
								property: BeanProperty,
								keyDeserializer: KeyDeserializer,
								elementTypeDeserializer: TypeDeserializer,
								elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
		null
	}

	def findTreeNodeDeserializer(nodeType: Class[_ <: JsonNode],
								 config: DeserializationConfig,
								 property: BeanProperty): JsonDeserializer[_] = {
		null
	}

	def findBeanDeserializer(javaType: JavaType,
							 config: DeserializationConfig,
							 provider: DeserializerProvider,
							 beanDesc: BeanDescription,
							 property: BeanProperty) = {

		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null;

		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
			// Note: Since Map also Iterable: implements, make sure we're checking Map before Iterable.
			deserializer = findMapDeserializer(javaType, config, provider, beanDesc, property)
		} else if (classOf[Iterable[Any]].isAssignableFrom(clazz)) {
			deserializer = findIterableDeserializer(javaType, config, provider, beanDesc, property)
		} else if (classOf[Option[Any]].isAssignableFrom(clazz)) {
			// Todo:
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

	private def findIterableDeserializer(javaType: JavaType,
										 config: DeserializationConfig,
										 provider: DeserializerProvider,
										 beanDesc: BeanDescription,
										 property: BeanProperty) = {
		val sig = javaType.getGenericSignature
		// Todo: Hacking indicies: parameter, make sure they are there.
		val contentType = javaType.containedType(0)
		val contentDeser = provider.findValueDeserializer(config, contentType, property)
		val valueTypeDeser = null // TODO: will this be a problem?
		null //new ScalaListDeserializer(javaType, contentDeser, valueTypeDeser)
	}
}
