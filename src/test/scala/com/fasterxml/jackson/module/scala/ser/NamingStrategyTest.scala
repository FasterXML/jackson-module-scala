package com.fasterxml.jackson
package module.scala
package ser

import java.io.ByteArrayOutputStream

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Outcome

import scala.beans.BeanProperty

class PojoWrittenInScala {
  @BeanProperty var fooBar: String = ""
}

class NamingStrategyTest extends FixtureAnyFlatSpec with Matchers {

  type FixtureParam = ObjectMapper

  protected def withFixture(test: OneArgTest): Outcome = {
    val builder = JsonMapper.builder()
    val settings = builder.baseSettings().`with`(PropertyNamingStrategies.SNAKE_CASE)
    val mapper = builder.baseSettings(settings).addModule(DefaultScalaModule).build()
    test(mapper)
  }

  "DefaultScalaModule" should "correctly handle naming strategies" in { mapper =>
    val bytes = new ByteArrayOutputStream()
    mapper.writeValue(bytes, new PojoWrittenInScala)
    bytes.close()
    bytes.toString should include("foo_bar")
    val pojo = mapper.readValue(bytes.toByteArray, classOf[PojoWrittenInScala])
    pojo.getFooBar() shouldEqual ""
  }
}
