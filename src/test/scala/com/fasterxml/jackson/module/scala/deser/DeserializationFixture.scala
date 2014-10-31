package com.fasterxml.jackson.module.scala.deser

import org.scalatest.{Succeeded, Outcome, Matchers, fixture}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

class DeserializationFixture extends fixture.FlatSpec with Matchers {

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  def withFixture(test: OneArgTest): Outcome =
  {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    test(mapper)
  }

}
