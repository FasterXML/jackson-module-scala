package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.BaseFixture
import com.fasterxml.jackson.databind.JsonSerializable
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.core.JsonGenerator

import scala.collection.immutable.AbstractMap

object JsonSerializableSpec {
  class SerializableMap extends AbstractMap[String, String] with JsonSerializable {

    // Members declared in scala.collection.MapLike
    def get(key: String): Option[String] = None
    def iterator: Iterator[(String, String)] = throw new IllegalArgumentException("This shouldn't get called")

    override def serialize(jgen: JsonGenerator, provider: SerializerProvider): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
      serialize(jgen, provider)
    }

    override def removed(key: String): Map[String, String] = ???

    override def updated[V1 >: String](key: String, value: V1): Map[String, V1] = ???
  }

  class SerializableIterable extends Iterable[String] with JsonSerializable {
    override def iterator: Iterator[String] = throw new IllegalArgumentException("This shouldn't get called")
    override def serialize(jgen: JsonGenerator, provider: SerializerProvider): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
      serialize(jgen, provider)
    }
  }

  class SerializableIterator extends Iterator[String] with JsonSerializable {
    override def serialize(jgen: JsonGenerator, provider: SerializerProvider): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
      serialize(jgen, provider)
    }

    override def hasNext: Boolean = ???

    override def next(): String = ???
  }
}

class JsonSerializableSpec extends BaseFixture {

  it should "use serialize method in JsonSerializable (Map)" in { mapper =>
    mapper.writeValueAsString(new JsonSerializableSpec.SerializableMap()) shouldBe "10"
  }
  it should "use serialize method in JsonSerializable (Iterable)" in { mapper =>
    mapper.writeValueAsString(new JsonSerializableSpec.SerializableIterable()) shouldBe "10"
  }
  it should "use serialize method in JsonSerializable (Iterator)" in { mapper =>
    mapper.writeValueAsString(new JsonSerializableSpec.SerializableIterator()) shouldBe "10"
  }
}
