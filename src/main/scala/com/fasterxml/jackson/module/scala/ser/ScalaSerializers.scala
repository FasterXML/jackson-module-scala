package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import ser.CustomSerializerFactory
import org.codehaus.jackson.`type`.JavaType

class ScalaSerializers extends CustomSerializerFactory with Serializers {

	addGenericMapping(classOf[Iterable[Any]], new ScalaIterableSerializer)
	addGenericMapping(classOf[Map[String,Any]], new ScalaImmutableMapSerializer)
	addGenericMapping(classOf[Option[Any]], new ScalaOptionSerializer)
	addGenericMapping(classOf[scala.Enumeration$Val], (new ScalaEnumerationSerializer).asInstanceOf[JsonSerializer[Object]])

	override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDescription: BeanDescription, property: BeanProperty) = {
		createSerializer(config, javaType, property)
	}
}