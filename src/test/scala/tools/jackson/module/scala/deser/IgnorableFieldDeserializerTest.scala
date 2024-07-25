package tools.jackson.module.scala.deser

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.module.scala.deser.IgnorableFieldDeserializerTest.ExtractFields
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

object IgnorableFieldDeserializerTest {
  case class ExtractFields(s: String, i: Int)
}

class IgnorableFieldDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with the DefaultScalaModule" should "fail if field is not expected (DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES enabled)" in {
    val mapper = newBuilder
      .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build()
    intercept[UnrecognizedPropertyException] {
      mapper.readValue(genJson(100), classOf[ExtractFields])
    }
  }

  it should "succeed if field is not expected (DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES disabled)" in {
    val mapper = newBuilder
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build()
    val ef = mapper.readValue(genJson(1000), classOf[ExtractFields])
    ef.s shouldEqual "s"
    ef.i shouldEqual 1
  }

  private def genJson(size: Int): String = {
    s"""{"s":"s","n":${"7" * size},"i":1}"""
  }

}
