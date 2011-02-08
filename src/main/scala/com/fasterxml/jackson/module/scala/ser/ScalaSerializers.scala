package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson.`type`.JavaType

/**
 * The implementation of these Scala*Serializers is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
class ScalaSerializers extends Serializers {

	override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDescription: BeanDescription, property: BeanProperty) = {
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
}
