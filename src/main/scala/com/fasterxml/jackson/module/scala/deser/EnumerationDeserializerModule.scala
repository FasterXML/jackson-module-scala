package com.fasterxml.jackson.module.scala
package deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.deser.{ContextualDeserializer, ContextualKeyDeserializer, Deserializers, KeyDeserializers}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.util.EnumResolver

private trait ContextualEnumerationDeserializer extends ContextualDeserializer {
  self: JsonDeserializer[Enumeration#Value] =>

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) : JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer = {
    EnumResolver(property).map(r => new AnnotatedEnumerationDeserializer(r)).getOrElse(this)
  }

}

/**
 * This class is mostly legacy logic to be deprecated/removed in 3.0
 */
private class EnumerationDeserializer(theType:JavaType) extends JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
  override def deserialize(jp:JsonParser, ctxt:DeserializationContext): Enumeration#Value = {
    if (jp.getCurrentToken != JsonToken.START_OBJECT) {
      ctxt.handleUnexpectedToken(theType.getRawClass, jp).asInstanceOf[Enumeration#Value]
    } else {
      val (eclass, eclassName) = parsePair(jp)
      if (eclass != "enumClass") {
        ctxt.handleUnexpectedToken(theType.getRawClass, jp).asInstanceOf[Enumeration#Value]
      } else {
        val (value, valueValue) = parsePair(jp)
        if (value != "value") {
          ctxt.handleUnexpectedToken(theType.getRawClass, jp).asInstanceOf[Enumeration#Value]
        } else {
          jp.nextToken()
          Class.forName(eclassName + "$").getField("MODULE$").get(None.orNull).asInstanceOf[Enumeration].withName(valueValue)
        }
      }
    }
  }

  private def parsePair(jp: JsonParser): (String, String) = (nextToken(jp), nextToken(jp))
  private def nextToken(jp: JsonParser): String = {
    jp.nextToken
    jp.getText
  }
}

private class AnnotatedEnumerationDeserializer(r: EnumResolver) extends JsonDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Enumeration#Value = {
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => r.getEnum(jp.getValueAsString)
      case _ => ctxt.handleUnexpectedToken(r.getEnumClass, jp).asInstanceOf[Enumeration#Value]
    }
  }
}

private object EnumerationDeserializerResolver extends Deserializers.Base {

  private val ENUMERATION = classOf[scala.Enumeration#Value]

  override def findBeanDeserializer(javaType: JavaType,
          config: DeserializationConfig,
          beanDesc: BeanDescription) = {

    val clazz = javaType.getRawClass
    if (ENUMERATION.isAssignableFrom(clazz)) {
      new EnumerationDeserializer(javaType)
    } else {
      None.orNull
    }
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

  private val ENUMERATION = classOf[scala.Enumeration#Value]

  def findKeyDeserializer(tp: JavaType, cfg: DeserializationConfig, desc: BeanDescription): KeyDeserializer = {
    if (ENUMERATION.isAssignableFrom(tp.getRawClass)) {
      new EnumerationKeyDeserializer(None)
    }
    else None.orNull
  }
}

trait EnumerationDeserializerModule extends JacksonModule {
  this += { ctxt =>
    ctxt.addDeserializers(EnumerationDeserializerResolver)
    ctxt.addKeyDeserializers(EnumerationKeyDeserializers)
  }
}
