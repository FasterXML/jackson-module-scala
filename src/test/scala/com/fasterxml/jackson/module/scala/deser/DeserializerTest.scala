package com.fasterxml.jackson.module.scala.deser

import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JacksonTest

trait DeserializerTest extends JacksonTest {

  private lazy val deserializerMapper = newMapper

  def serialize(o: AnyRef): String = deserializerMapper.writeValueAsString(o)

  def deserialize[T](value: String, typeReference: TypeReference[T]): T =
    deserializerMapper.readValue(value, typeReference)

  def deserialize[T](value: String, clazz: Class[T]): T =
    deserializerMapper.readValue(value, clazz)

  def deserialize[T](value: String, referenceType: Class[T], argType: Class[_]): T =
    deserialize(value, referenceType, Seq(argType))

  def deserialize[T](value: String, referenceType: Class[T], argTypes: Seq[Class[_]]): T = {
    val pt = new ParameterizedType {
      override def getRawType: Class[_] = referenceType

      override def getActualTypeArguments: Array[Type] = argTypes.toArray

      override def getOwnerType: Null = null
    }
    deserializerMapper.readValue(value, typeReference(pt))
  }

  @deprecated("need to stop using manifests because they are not supported in Scala3", "2.12.1")
  def deserializeWithManifest[T: Manifest](value: String) : T =
    deserializerMapper.readValue(value, typeReference[T])

  private def typeReference[T: Manifest]: TypeReference[T] =
    typeReference(typeFromManifest(manifest[T]))

  private def typeReference[T](t: Type): TypeReference[T] = new TypeReference[T] {
    override def getType: Type = t
  }

  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) { m.runtimeClass }
    else new ParameterizedType {
      override def getRawType: Class[_] = m.runtimeClass

      override def getActualTypeArguments: Array[Type] = m.typeArguments.map(typeFromManifest).toArray

      override def getOwnerType: Null = null
    }
  }

}
