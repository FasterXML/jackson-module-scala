package tools.jackson.module.scala.ser

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.{JacksonSerializable, SerializationContext}
import tools.jackson.databind.jsontype.TypeSerializer

import scala.collection.immutable.AbstractMap

class SerializableMap extends AbstractMap[String, String] with JacksonSerializable {

  // Members declared in scala.collection.MapLike
  def +[B1 >: String](kv: (String, B1)) = this
  def -(key: String): Map[String,String] = this
  def get(key: String): Option[String] = None
  def iterator: Iterator[(String, String)] = throw new IllegalArgumentException("This shouldn't get called")

  override def serialize(jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    jgen.writeNumber(10)
  }
  override def serializeWithType(jgen: JsonGenerator, serializationContext: SerializationContext, typeSer: TypeSerializer): Unit = {
    serialize(jgen, serializationContext)
  }
}
