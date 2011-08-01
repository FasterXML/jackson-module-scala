package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map._
import org.codehaus.jackson.`type`.JavaType

class ScalaSerializers extends Serializers.None {

  override def findSerializer(config: SerializationConfig,
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

}
