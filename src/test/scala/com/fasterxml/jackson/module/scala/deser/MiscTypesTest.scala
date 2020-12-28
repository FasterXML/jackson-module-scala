package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.`type`.TypeReference

import java.util.UUID
import com.fasterxml.jackson.module.scala.DefaultScalaModule

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
