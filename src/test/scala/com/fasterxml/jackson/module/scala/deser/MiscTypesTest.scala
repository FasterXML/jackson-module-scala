package com.fasterxml.jackson.module.scala.deser

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.UUID
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MiscTypesTest extends DeserializerTest {

  def module = DefaultScalaModule

  "Scala Module" should "deserialize UUID" in {
    val data: Seq[UUID] = Stream.continually(UUID.randomUUID).take(4).toList

    val json = newMapper.writeValueAsString(data)
    val read = deserialize[List[UUID]](json)

    read shouldBe (data)
  }

}
