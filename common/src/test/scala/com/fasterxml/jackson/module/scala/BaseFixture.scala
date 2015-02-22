package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.scalatest.{Matchers, Outcome, fixture}

class BaseFixture extends fixture.FlatSpec with Matchers {

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  def withFixture(test: OneArgTest): Outcome =
  {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    test(mapper)
  }

}
