package com.fasterxml.jackson
package module.scala
package deser

import com.fasterxml.jackson.core.JsonToken.{START_ARRAY, VALUE_NUMBER_FLOAT, VALUE_NUMBER_INT, VALUE_STRING}
import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.{JsonDeserializer, BeanDescription, DeserializationConfig, JavaType, DeserializationContext}
import com.fasterxml.jackson.databind.DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer

object BigDecimalDeserializer extends StdScalarDeserializer[BigDecimal](classOf[BigDecimal]) {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    val t = jp.getCurrentToken
    t match {
      case VALUE_NUMBER_INT | VALUE_NUMBER_FLOAT | VALUE_STRING =>
        val text = jp.getText.trim
        if (text.length == 0)
          null
        else
          BigDecimal(text)

      case START_ARRAY if ctxt.isEnabled(UNWRAP_SINGLE_VALUE_ARRAYS) =>
        jp.nextToken()
        val value = deserialize(jp, ctxt)
        if (jp.nextToken() != JsonToken.END_ARRAY) {
          throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, "Attempted to unwrap single value array for single 'BigDecimal' value but there was more than a single value in the array")
        }
        value

      case _ =>
        throw ctxt.mappingException(_valueClass, t)
    }
  }
}

trait ScalaStdValueInstantiatorsModule extends JacksonModule {
  this += (_.addDeserializers(new Deserializers.Base {
    override def findBeanDeserializer(`type`: JavaType,
                                      config: DeserializationConfig,
                                      beanDesc: BeanDescription): JsonDeserializer[_] = {
      if (`type`.getRawClass == BigDecimalDeserializer.handledType) {
        return BigDecimalDeserializer
      }

      null
    }
  }))
}
