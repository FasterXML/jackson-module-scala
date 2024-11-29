package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.{JacksonSerializable, SerializationContext}

import scala.collection.immutable.AbstractMap

class SerializableMap extends AbstractMap[String, String] with JacksonSerializable {

  // Members declared in scala.collection.MapLike
  def get(key: String): Option[String] = None
  def iterator: Iterator[(String, String)] = throw new IllegalArgumentException("This shouldn't get called")

  override def serialize(jgen: JsonGenerator, provider: SerializationContext): Unit = {
    jgen.writeNumber(10)
  }
  override def serializeWithType(jgen: JsonGenerator, provider: SerializationContext, typeSer: TypeSerializer): Unit = {
    serialize(jgen, provider)
  }

  override def removed(key: String): Map[String, String] = ???

  override def updated[V1 >: String](key: String, value: V1): Map[String, V1] = ???
}
