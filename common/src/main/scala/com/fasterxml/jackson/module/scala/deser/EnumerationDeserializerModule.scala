package com.fasterxml.jackson.module.scala
package deser

import util.EnumResolver
import util.Implicits._

import com.fasterxml.jackson
import jackson.core
import core.{JsonToken, JsonParser}

import jackson.databind
import databind.{deser, KeyDeserializer, BeanDescription, DeserializationConfig, JavaType, BeanProperty, DeserializationContext, JsonDeserializer}
import deser.{ContextualKeyDeserializer, KeyDeserializers, ContextualDeserializer, Deserializers}

import java.lang.reflect.ParameterizedType

private trait ContextualEnumerationDeserializer extends ContextualDeserializer {
  self: JsonDeserializer[Enumeration#Value] =>

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) : JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer = {
    EnumResolver(property).map(r => new AnnotatedEnumerationDeserializer(r)).getOrElse(this)
  }

}

/**
 * This class is mostly legacy logic to be deprecated/removed in 3.0
 */
private class EnumerationDeserializer(`type`:JavaType) extends JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
	override def deserialize(jp:JsonParser, ctxt:DeserializationContext): Enumeration#Value = {
		if (jp.getCurrentToken != JsonToken.START_OBJECT)
			throw ctxt.mappingException(`type`.getRawClass)
		val (eclass,eclassName) = parsePair(jp)
		if (eclass != "enumClass")
			throw ctxt.mappingException(`type`.getRawClass)
		val (value, valueValue) = parsePair(jp)
		if (value != "value")
			throw ctxt.mappingException(`type`.getRawClass)
		jp.nextToken()
		Class.forName(eclassName).getMethod("withName",classOf[String]).invoke(null,valueValue).asInstanceOf[Enumeration#Value]
	}

	private def parsePair( jp:JsonParser ) = ({jp.nextToken; jp.getText}, {jp.nextToken; jp.getText})
}

private class AnnotatedEnumerationDeserializer(r: EnumResolver) extends JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Enumeration#Value = {
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => r.getEnum(jp.getValueAsString)
      case _ => throw ctxt.mappingException(r.getEnumClass)
    }
  }
}

private object EnumerationDeserializerResolver extends Deserializers.Base {

  override def findBeanDeserializer(javaType: JavaType,
          config: DeserializationConfig,
          beanDesc: BeanDescription) = {

		val clazz = javaType.getRawClass
		var deserializer : JsonDeserializer[_] = null

		if (classOf[scala.Enumeration#Value].isAssignableFrom(clazz)) {
			deserializer = new EnumerationDeserializer(javaType)
		}

		deserializer
	}

}

private class EnumerationKeyDeserializer(r: Option[EnumResolver]) extends KeyDeserializer with ContextualKeyDeserializer {

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newResolver = EnumResolver(property)
    if (newResolver != r) new EnumerationKeyDeserializer(newResolver) else this
  }

  def deserializeKey(s: String, ctxt: DeserializationContext): Enumeration#Value = {
    if (r.isDefined) {
      return r.get.getEnum(s)
    }

    throw ctxt.mappingException("Need @JsonScalaEnumeration to determine key type")
  }

}

private object EnumerationKeyDeserializers extends KeyDeserializers {
  def findKeyDeserializer(tp: JavaType, cfg: DeserializationConfig, desc: BeanDescription): KeyDeserializer = {
    if (classOf[scala.Enumeration#Value].isAssignableFrom(tp.getRawClass)) {
      new EnumerationKeyDeserializer(None)
    }
    else null
  }
}

trait EnumerationDeserializerModule extends JacksonModule {
  this += { ctxt => {
    ctxt.addDeserializers(EnumerationDeserializerResolver)
    ctxt.addKeyDeserializers(EnumerationKeyDeserializers)
  } }
}
