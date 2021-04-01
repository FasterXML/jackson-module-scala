package com.fasterxml.jackson
package module.scala
package ser

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy}
import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayOutputStream
import scala.beans.BeanProperty

class PojoWrittenInScala {
  @BeanProperty var fooBar: String = ""
}

class NamingStrategyTest extends FixtureAnyFlatSpec with Matchers {

  type FixtureParam = ObjectMapper

  protected def withFixture(test: OneArgTest): Outcome = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    test(mapper)
  }

  "DefaultScalaModule" should "correctly handle naming strategies" in { mapper =>
    val bytes = new ByteArrayOutputStream()
    mapper.writeValue(bytes, new PojoWrittenInScala)
    bytes.close()
    bytes.toString should include("foo_bar")
    val pojo = mapper.readValue(bytes.toByteArray, classOf[PojoWrittenInScala])
    pojo.fooBar shouldEqual ""
  }
}
