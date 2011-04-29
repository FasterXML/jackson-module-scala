package com.fasterxml.jackson.module.scala.deser

import java.lang.Class
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.`type`.{ArrayType, CollectionType, MapType}
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._

/**
 * The ScalaDeserialziers finds implementations of JsonDeserializer that can be used to parse JSON into Scala
 * collections, specifically json to Map and json to Iterable.
 */
class ScalaDeserializers  { //extends Deserializers {

	/*
	public JsonDeserializer<?> findArrayDeserializer(ArrayType type, DeserializationConfig config,
	DeserializerProvider provider,
	BeanProperty property,
	TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
	throws JsonMappingException;

	public JsonDeserializer<?> findCollectionDeserializer(CollectionType type, DeserializationConfig config,
	DeserializerProvider provider, BeanDescription beanDesc, BeanProperty property,
	TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
	throws JsonMappingException;

	public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config,
	DeserializerProvider provider, BeanDescription beanDesc, BeanProperty property,
	TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
	throws JsonMappingException;

	public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config,
	BeanDescription beanDesc, BeanProperty property)
	throws JsonMappingException;

	public JsonDeserializer<?> findMapDeserializer(MapType type, DeserializationConfig config,
	DeserializerProvider provider, BeanDescription beanDesc, BeanProperty property,
	KeyDeserializer keyDeserializer,
	TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
	throws JsonMappingException;

	public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type, DeserializationConfig config,
	DeserializerProvider provider, BeanDescription beanDesc, BeanProperty property,
	KeyDeserializer keyDeserializer,
	TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
	throws JsonMappingException;

	public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType, DeserializationConfig config,
	BeanProperty property)
	throws JsonMappingException;

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
		// Todo: Hacking parameter indicies, should probably introspect further
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
		// Todo: Hacking parameter indicies, make sure they are there.
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
	*/
}