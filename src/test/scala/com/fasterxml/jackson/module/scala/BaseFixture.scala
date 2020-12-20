package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseFixture extends FixtureAnyFlatSpec with Matchers {

  type FixtureParam = ObjectMapper

  def withFixture(test: OneArgTest): Outcome = {
    val builder = JsonMapper.builder().addModule(DefaultScalaModule)
    val mapper = builder.build()
    test(mapper)
  }
}
