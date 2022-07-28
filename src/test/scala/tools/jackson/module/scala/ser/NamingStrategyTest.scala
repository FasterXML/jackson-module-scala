package tools.jackson.module.scala.ser

import org.scalatest.Outcome
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import tools.jackson.module.scala.DefaultScalaModule

import java.io.ByteArrayOutputStream
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
    pojo.fooBar shouldEqual ""
  }
}
