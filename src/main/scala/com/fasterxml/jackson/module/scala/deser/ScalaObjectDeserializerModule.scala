package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ClassW

import scala.languageFeature.postfixOps
import scala.util.control.NonFatal

private class ScalaObjectDeserializer(value: Any) extends StdDeserializer[Any](classOf[Any]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Any = {
    // A Scala object holds no state, so whatever was written for it is consumed and discarded.
    // skipChildren stops at the end token matching the one it started on - not at the first one it
    // meets - and does nothing at all for a scalar, so a nested object no longer ends the value
    // early and a scalar no longer eats the tokens of whatever encloses it. Reading a scalar at the
    // root used to spin forever here: nextToken returns null at end of input, never END_OBJECT.
    //
    // A type deserializer that has read an As.PROPERTY type id hands over the parser already inside
    // the object, at the next property name, where skipChildren has nothing to skip: the rest of
    // that object is consumed here instead, up to the END_OBJECT the value is left at.
    if (p.currentToken() == JsonToken.FIELD_NAME) {
      var token = p.nextToken()
      while (token != null && token != JsonToken.END_OBJECT) {
        p.skipChildren()
        token = p.nextToken()
      }
    } else {
      p.skipChildren()
    }
    value
  }
}

private object ScalaObjectDeserializerResolver extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    ClassW(javaType.getRawClass).getModuleField.flatMap { field =>
      Option(field.get(null))
    }.map(new ScalaObjectDeserializer(_)).orNull
  }
}

trait ScalaObjectDeserializerModule extends JacksonModule {
  override def getModuleName: String = "ScalaObjectDeserializerModule"
  this += { _ addDeserializers ScalaObjectDeserializerResolver }
}

object ScalaObjectDeserializerModule extends ScalaObjectDeserializerModule
