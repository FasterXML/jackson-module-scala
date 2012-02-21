package com.fasterxml.jackson.module.scala.deser

import java.lang.reflect.{Type, ParameterizedType}

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.`type`.TypeReference;

import com.fasterxml.jackson.module.scala.JacksonTest

/**
 * Undocumented class.
 */

trait DeserializerTest extends JacksonTest {

  def deserialize[T: Manifest](value: String) : T =
    mapper.readValue(value, new TypeReference[T]() {
      override def getType = new ParameterizedType {
        val getActualTypeArguments = manifest[T].typeArguments.map(_.erasure.asInstanceOf[Type]).toArray
        val getRawType = manifest[T].erasure
        val getOwnerType = null
      }
    })

}