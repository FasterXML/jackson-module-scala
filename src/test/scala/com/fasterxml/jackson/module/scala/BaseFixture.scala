package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.scalatest.{Matchers, Outcome, fixture}

class BaseFixture extends fixture.FlatSpec with Matchers {

  type FixtureParam = ObjectMapper with ScalaObjectMapper

  object ScalaObjectMapper {
    //implicit def innerObj(o: Mixin) = o.obj

    def ::(o: JsonMapper) = new Mixin(o)
    final class Mixin private[ScalaObjectMapper](val obj: JsonMapper) extends JsonMapper with ScalaObjectMapper
  }

  def withFixture(test: OneArgTest): Outcome = {
    import ScalaObjectMapper._
    val builder = JsonMapper.builder().addModule(new DefaultScalaModule)
    val mapper = builder.build() :: ScalaObjectMapper
    test(mapper)
  }
}
