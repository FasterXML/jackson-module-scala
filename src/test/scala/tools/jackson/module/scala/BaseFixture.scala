package tools.jackson.module.scala

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
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
