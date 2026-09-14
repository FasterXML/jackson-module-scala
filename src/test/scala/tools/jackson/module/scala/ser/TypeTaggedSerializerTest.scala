package tools.jackson.module.scala.ser

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.impl.UnknownSerializer
import tools.jackson.databind.{SerializationFeature, ValueSerializer}
import tools.jackson.module.scala.{BaseSpec, DefaultScalaModule}

object TypeTaggedSerializerTest {
  case object Lone
  case class Holder(lone: Lone.type)
}

class TypeTaggedSerializerTest extends BaseSpec {
  import TypeTaggedSerializerTest._

  behavior of "TypeTaggedSerializer"

  // Jackson gives a type it finds nothing to write for an UnknownSerializer rather than a bean
  // serializer. Whether a case object is such a type depends on the standard library it was built
  // against, so the delegate is chosen here rather than left to introspection.
  it should "write only the tag when Jackson found nothing to write for the type" in {
    val tagged = new TypeTaggedSerializer("@type", "Lone",
      new UnknownSerializer(Lone.getClass).asInstanceOf[ValueSerializer[AnyRef]])
    val module = new SimpleModule().addSerializer(Lone.getClass.asInstanceOf[Class[Lone.type]], tagged)
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .addModule(module)
      .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .build()

    mapper.writeValueAsString(Lone) shouldEqual """{"@type":"Lone"}"""
    mapper.writeValueAsString(Holder(Lone)) shouldEqual """{"lone":{"@type":"Lone"}}"""
  }
}
