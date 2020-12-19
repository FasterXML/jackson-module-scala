package com.fasterxml.jackson.module.scala.deser

import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JacksonTest

trait DeserializerTest extends JacksonTest {

  private lazy val deserializerMapper = newMapper

  def serialize(o: AnyRef): String = deserializerMapper.writeValueAsString(o)

  def deserialize[T](value: String, clazz: Class[T]): T =
    deserializerMapper.readValue(value, clazz)

  def deserializeWithManifest[T: Manifest](value: String) : T =
    deserializerMapper.readValue(value, typeReference[T])

  def typeReference[T: Manifest]: TypeReference[T] = new TypeReference[T] {
    override def getType: Type = typeFromManifest(manifest[T])
  }

  private [this] def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) { m.runtimeClass }
    else new ParameterizedType {
      override def getRawType: Class[_] = m.runtimeClass

      override def getActualTypeArguments: Array[Type] = m.typeArguments.map(typeFromManifest).toArray

      override def getOwnerType: Null = null
    }
  }

}
