package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{JsonSerializable, SerializerProvider}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer

import scala.collection.immutable.AbstractMap

class SerializableMap extends AbstractMap[String, String] with JsonSerializable {

  // Members declared in scala.collection.MapLike
  def +[B1 >: String](kv: (String, B1)) = this
  def -(key: String): Map[String,String] = this
  def get(key: String): Option[String] = None
  def iterator: Iterator[(String, String)] = throw new IllegalArgumentException("This shouldn't get called")

  override def serialize(jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeNumber(10)
  }
  override def serializeWithType(jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
    serialize(jgen, provider)
  }
}
