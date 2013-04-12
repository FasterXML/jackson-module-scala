package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonToken, JsonParser}

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, Deserializers}

import com.fasterxml.jackson.module.scala.{JsonScalaEnumeration, JacksonModule}
import com.fasterxml.jackson.module.scala.util.Implicts._

import java.lang.reflect.ParameterizedType

trait ContextualEnumerationDeserializer extends ContextualDeserializer {
  self: JsonDeserializer[Object] =>

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) : JsonDeserializer[Object] = {
    Option(property)
      .optMap(_.getAnnotation(classOf[JsonScalaEnumeration]))
      .map(a => new AnnotatedEnumerationDeserializer(enum(a.value())))
      .getOrElse(this)
  }

  private [this] def enum(cls: Class[_]): Enumeration = {
    val typeArgs = typeRef(cls).getActualTypeArguments
    typeArgs(0).asInstanceOf[Class[_]].getField("MODULE$").get(cls).asInstanceOf[Enumeration]
  }

  private [this] def typeRef(cls: Class[_]): ParameterizedType = {
    cls.getGenericSuperclass.asInstanceOf[ParameterizedType]
  }
}

private class EnumerationDeserializer(`type`:JavaType) extends JsonDeserializer[Object] with ContextualEnumerationDeserializer {
	override def deserialize(jp:JsonParser, ctxt:DeserializationContext) = {
		if (jp.getCurrentToken != JsonToken.START_OBJECT)
			throw ctxt.mappingException(`type`.getRawClass)
		val (eclass,eclassName) = parsePair(jp)
		if (eclass != "enumClass")
			throw ctxt.mappingException(`type`.getRawClass)
		val (value, valueValue) = parsePair(jp)
		if (value != "value")
			throw ctxt.mappingException(`type`.getRawClass)
		jp.nextToken()
		Class.forName(eclassName).getMethod("withName",classOf[String]).invoke(null,valueValue)
	}

	private def parsePair( jp:JsonParser ) = ({jp.nextToken; jp.getText}, {jp.nextToken; jp.getText})
}

private class AnnotatedEnumerationDeserializer(enum: Enumeration) extends JsonDeserializer[Object] with ContextualEnumerationDeserializer {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Object = {
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => enum.withName(jp.getValueAsString)
      case _ => ctxt.mappingException(classOf[enum.type#Value])
    }
  }
}

private object EnumerationDeserializerResolver extends Deserializers.Base {

  override def findBeanDeserializer(javaType: JavaType,
          config: DeserializationConfig,
          beanDesc: BeanDescription) = {

		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null;

		if (classOf[scala.Enumeration#Value].isAssignableFrom(clazz)) {
			deserializer = new EnumerationDeserializer(javaType)
		}

		deserializer
	}

}

trait EnumerationDeserializerModule extends JacksonModule {
  this += EnumerationDeserializerResolver
}
