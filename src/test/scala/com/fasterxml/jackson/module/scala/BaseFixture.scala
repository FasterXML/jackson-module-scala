package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseFixture extends FixtureAnyFlatSpec with Matchers {

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  def withFixture(test: OneArgTest): Outcome = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    test(mapper)
  }
}
