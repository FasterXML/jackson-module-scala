package com.fasterxml.jackson.module.scala

import com.fasterxml.jackson.annotation.{JsonProperty, JsonAutoDetect}
import com.fasterxml.jackson.module.scala.ser.SerializerTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

object VisibilityTest {
  @JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE)
  trait NotVisible

  abstract class AFieldVisibility extends NotVisible {
    val foo = "not visible"
    @JsonProperty
    val bar = "visible"
  }

  class FieldVisibility extends AFieldVisibility {
    val baz = "not visible"
    @JsonProperty
    val zip = "visible"
  }

  abstract class AMethodVisibility extends NotVisible {
    def foo = "not visible"
    @JsonProperty
    def bar = "visible"
  }

  class MethodVisibility extends AFieldVisibility {
    def baz = "not visible"
    @JsonProperty
    def zip = "visible"
  }
}

@RunWith(classOf[JUnitRunner])
class VisibilityTest extends SerializerTest {
  import VisibilityTest._

  lazy val module = DefaultScalaModule

  "An ObjectMapper" should "respect field visibility" in {
    val p = new FieldVisibility()
    serialize(p) should be ("""{"bar":"visible","zip":"visible"}""")
  }

  "An ObjectMapper" should "respect method visibility" in {
    val p = new MethodVisibility()
    serialize(p) should be ("""{"bar":"visible","zip":"visible"}""")
  }
}
