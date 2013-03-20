package com.fasterxml.jackson.module.scala.deser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.UUID
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MiscTypesTest extends DeserializerTest with FlatSpec with ShouldMatchers {

  def module = DefaultScalaModule

  "Scala Module" should "deserialize UUID" in {
    val data: Seq[UUID] = Stream.continually(UUID.randomUUID).take(4).toList

    val json = mapper.writeValueAsString(data)
    val read = deserialize[List[UUID]](json)

    read should be === (data)
  }

}
