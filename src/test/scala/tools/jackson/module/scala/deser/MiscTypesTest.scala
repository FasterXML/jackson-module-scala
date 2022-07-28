package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.module.scala.DefaultScalaModule

import java.util.UUID

class MiscTypesTest extends DeserializerTest {

  def module: DefaultScalaModule.type = DefaultScalaModule

  "Scala Module" should "deserialize UUID" in {
    val data: Seq[UUID] = Stream.continually(UUID.randomUUID).take(4).toList

    val mapper = newMapper
    val json = mapper.writeValueAsString(data)
    val read = mapper.readValue(json, new TypeReference[List[UUID]]{})

    read shouldBe data
  }
}
