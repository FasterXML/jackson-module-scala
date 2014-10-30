package com.fasterxml.jackson.module.scala.ser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Outcome, Matchers, fixture}
import com.fasterxml.jackson.databind.{PropertyNamingStrategy, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.ByteArrayOutputStream
import com.google.common.base.Optional
import scala.beans.BeanProperty
import javax.annotation.Nonnull

class PojoWrittenInScala {
  @Nonnull @BeanProperty var optFoo: Optional[String] = Optional.absent()
  @Nonnull @BeanProperty var bar: Int = 0
}

@RunWith(classOf[JUnitRunner])
class NamingStrategyTest extends fixture.FlatSpec with Matchers {

  type FixtureParam = ObjectMapper

  protected def withFixture(test: OneArgTest): Outcome = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
    test(mapper)
  }

  "DefaultScalaModule" should "correctly handle naming strategies" in { mapper =>
    val bytes = new ByteArrayOutputStream()
    mapper.writeValue(bytes, new PojoWrittenInScala)
  }

}
