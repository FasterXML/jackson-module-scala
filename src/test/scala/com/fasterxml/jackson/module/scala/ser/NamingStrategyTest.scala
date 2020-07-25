package com.fasterxml.jackson
package module.scala
package ser

import java.io.ByteArrayOutputStream

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import com.google.common.base.Optional
import org.junit.runner.RunWith
import org.scalatest.{Matchers, Outcome, fixture}
import org.scalatestplus.junit.JUnitRunner

import scala.beans.BeanProperty

class PojoWrittenInScala {
  @BeanProperty var optFoo: Optional[String] = Optional.absent()
  @BeanProperty var bar: Int = 0
}

@RunWith(classOf[JUnitRunner])
class NamingStrategyTest extends fixture.FlatSpec with Matchers {

  type FixtureParam = ObjectMapper

  protected def withFixture(test: OneArgTest): Outcome = {
    val builder = JsonMapper.builder()
    val settings = builder.baseSettings().`with`(PropertyNamingStrategy.SNAKE_CASE)
    val mapper = builder.baseSettings(settings).addModule(DefaultScalaModule).build()
    test(mapper)
  }

  "DefaultScalaModule" should "correctly handle naming strategies" in { mapper =>
    val bytes = new ByteArrayOutputStream()
    mapper.writeValue(bytes, new PojoWrittenInScala)
  }
}
