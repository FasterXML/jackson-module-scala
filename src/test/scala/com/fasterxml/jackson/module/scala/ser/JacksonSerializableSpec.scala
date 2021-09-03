package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.module.scala.BaseFixture
import com.fasterxml.jackson.databind.{JacksonSerializable, SerializerProvider}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.core.JsonGenerator

object JacksonSerializableSpec {
  class SerializableIterable extends JacksonSerializable.Base with Iterable[String] {
    override def iterator: Iterator[String] = throw new IllegalArgumentException("This shouldn't get called")
    override def serialize(jgen: JsonGenerator, provider: SerializerProvider): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
      serialize(jgen, provider)
    }
  }

  class SerializableIterator extends JacksonSerializable.Base with Iterator[String] {
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

class JacksonSerializableSpec extends BaseFixture {

  it should "use serialize method in JsonSerializable (Map)" in { mapper =>
    mapper.writeValueAsString(new SerializableMap()) shouldBe "10"
  }
  it should "use serialize method in JsonSerializable (Iterable)" in { mapper =>
    mapper.writeValueAsString(new JacksonSerializableSpec.SerializableIterable()) shouldBe "10"
  }
  it should "use serialize method in JsonSerializable (Iterator)" in { mapper =>
    mapper.writeValueAsString(new JacksonSerializableSpec.SerializableIterator()) shouldBe "10"
  }
  it should "use serialize method in JsonSerializable (Option[Iterable])" in { mapper =>
    mapper.writeValueAsString(Some(new JacksonSerializableSpec.SerializableIterator())) shouldBe "10"
  }
}
