package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.`type`._

/**
 * The implementation of these Scala*Serializers is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
class ScalaSerializers extends Serializers {
	def findSerializer(config: SerializationConfig,
					   javaType: JavaType,
					   beanDescription: BeanDescription,
					   beanProperty: BeanProperty): JsonSerializer[_] = {
		val clazz = javaType.getRawClass

		if (classOf[collection.Map[String, Any]].isAssignableFrom(clazz)) {
			new ScalaMapSerializer
		} else if (classOf[Iterable[Any]].isAssignableFrom(clazz)) {
			new ScalaIterableSerializer
		} else if (classOf[Option[Any]].isAssignableFrom(clazz)) {
			new ScalaOptionSerializer
		} else if (classOf[scala.Enumeration$Val].isAssignableFrom(clazz)) {
			new ScalaEnumerationSerializer
		} else {
			null
		}
	}

	def findArraySerializer(config: SerializationConfig,
							arrayType: ArrayType,
							beanDescription: BeanDescription,
							beanProperty: BeanProperty,
							elementTypeSerializer: TypeSerializer,
							elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

		null
	}

	def findCollectionSerializer(config: SerializationConfig,
								 collectionType: CollectionType,
								 beanDescription: BeanDescription,
								 beanProperty: BeanProperty,
								 elementTypeSerializer: TypeSerializer,
								 elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

		null
	}

	def findCollectionLikeSerializer(config: SerializationConfig,
									 collectionType: CollectionLikeType,
									 beanDescription: BeanDescription,
									 beanProperty: BeanProperty,
									 elementTypeSerializer: TypeSerializer,
									 elementSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

		null
	}

	def findMapSerializer(config: SerializationConfig,
						  mapType: MapType,
						  beanDescription: BeanDescription,
						  beanProperty: BeanProperty,
						  keySerializer: JsonSerializer[Object],
						  elementTypeSerializer: TypeSerializer,
						  elementValueSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

		null
	}

	def findMapLikeSerializer(config: SerializationConfig,
							  mapLikeType: MapLikeType,
							  beanDescription: BeanDescription,
							  beanProperty: BeanProperty,
							  keySerializer: JsonSerializer[Object],
							  elementTypeSerializer: TypeSerializer,
							  elementValueSerializer: JsonSerializer[Object]): JsonSerializer[_] = {

		null
	}

	/*
	override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDescription: BeanDescription, property: BeanProperty) = {
	}
	*/
}
