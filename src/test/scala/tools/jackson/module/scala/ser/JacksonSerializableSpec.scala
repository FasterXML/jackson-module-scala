package tools.jackson.module.scala.ser

import tools.jackson.databind.{JacksonSerializable, SerializationContext}
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.core.JsonGenerator
import tools.jackson.module.scala.BaseFixture

object JacksonSerializableSpec {
  class SerializableIterable extends JacksonSerializable.Base with Iterable[String] {
    override def iterator: Iterator[String] = throw new IllegalArgumentException("This shouldn't get called")
    override def serialize(jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, serializationContext: SerializationContext, typeSer: TypeSerializer): Unit = {
      serialize(jgen, serializationContext)
    }
  }

  class SerializableIterator extends JacksonSerializable.Base with Iterator[String] {
    override def serialize(jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
      jgen.writeNumber(10)
    }
    override def serializeWithType(jgen: JsonGenerator, serializationContext: SerializationContext, typeSer: TypeSerializer): Unit = {
      serialize(jgen, serializationContext)
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
