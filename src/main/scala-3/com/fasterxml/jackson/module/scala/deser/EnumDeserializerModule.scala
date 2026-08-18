package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.{Deserializers, KeyDeserializers}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule

import java.lang.reflect.InvocationTargetException
import scala.languageFeature.postfixOps
import scala.reflect.Enum
import scala.util.Try

private object EnumDeserializerShared {
  val IntClass = classOf[Int]
  val StringClass = classOf[String]
  val EnumClass = classOf[Enum]

  def tryValueOf(clz: Class[_], key: String): Option[_] = {
    Try(clz.getMethod("valueOf", EnumDeserializerShared.StringClass)).toOption.map { method =>
      method.invoke(None.orNull, key)
    }
  }

  def matchBasedOnOrdinal(clz: Class[_], key: String): Option[_] = {
    Try(clz.getMethod("fromOrdinal", IntClass)).toOption.flatMap { method =>
      var i = 0
      var matched: Option[_] = None
      var complete = false
      while (!complete) {
        try {
          val enumValue = method.invoke(None.orNull, i)
          if (enumValue.toString == key) {
            matched = Some(enumValue)
            complete = true
          }
        } catch {
          case _: NoSuchElementException => {
            matched = None
            complete = true
          }
          case itex: InvocationTargetException => {
            Option(itex.getCause) match {
              case Some(e) if e.isInstanceOf[NoSuchElementException] => {
                matched = None
                complete = true
              }
              case Some(e) => throw e
              case _ => throw itex
            }
          }
        }
        i += 1
      }
      matched
    }
  }
}

private case class EnumDeserializer[T <: Enum](clazz: Class[T]) extends StdDeserializer[T](clazz) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): T = {
    val result = Option(p.getValueAsString).flatMap { text =>
      Try {
        EnumDeserializerShared.tryValueOf(clazz, text)
          .orElse(EnumDeserializerShared.matchBasedOnOrdinal(clazz, text))
      }.toOption.flatten
    }
    result.getOrElse(throw new IllegalArgumentException(s"Failed to create Enum instance for ${p.getValueAsString}"))
      .asInstanceOf[T]
  }
}

private case class EnumKeyDeserializer[T <: Enum](clazz: Class[T]) extends KeyDeserializer {
  override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef = {
    val result = Try {
      EnumDeserializerShared.tryValueOf(clazz, key)
        .orElse(EnumDeserializerShared.matchBasedOnOrdinal(clazz, key))
    }.toOption.flatten
    val enumResult = result.getOrElse(throw new IllegalArgumentException(s"Failed to create Enum instance for $key"))
    enumResult.asInstanceOf[AnyRef]
  }
}

private object EnumDeserializerResolver extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[Enum] =
    if (EnumDeserializerShared.EnumClass isAssignableFrom javaType.getRawClass)
      EnumDeserializer(javaType.getRawClass.asInstanceOf[Class[Enum]])
    else None.orNull
}

private object EnumKeyDeserializerResolver extends KeyDeserializers {
  override def findKeyDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): KeyDeserializer =
    if (EnumDeserializerShared.EnumClass isAssignableFrom javaType.getRawClass)
      EnumKeyDeserializer(javaType.getRawClass.asInstanceOf[Class[Enum]])
    else None.orNull
}

trait EnumDeserializerModule extends JacksonModule {
  override def getModuleName: String = "EnumDeserializerModule"
  this += { _ addDeserializers EnumDeserializerResolver }
  this += { _ addKeyDeserializers EnumKeyDeserializerResolver }
}
