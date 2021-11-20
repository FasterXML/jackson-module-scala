package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object OptionWithBooleanDeserializerTest {
  case class AnnotatedOptionBoolean(@JsonDeserialize(contentAs = classOf[java.lang.Boolean]) valueBoolean: Option[Boolean])
  case class AnnotatedOptionPrimitiveBoolean(@JsonDeserialize(contentAs = classOf[Boolean]) valueBoolean: Option[Boolean])
  case class OptionBoolean(valueBoolean: Option[Boolean])
  case class OptionJavaBoolean(valueBoolean: Option[java.lang.Boolean])
}

class OptionWithBooleanDeserializerTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import OptionWithBooleanDeserializerTest._

  private def useOptionBoolean(v: Option[Boolean]): String = v.map(_.toString).getOrElse("null")
  private def useOptionJavaBoolean(v: Option[java.lang.Boolean]): String = v.map(_.toString).getOrElse("null")

  "JacksonModuleScala" should "deserialize AnnotatedOptionBoolean" in {
    val v1 = deserialize("""{"valueBoolean":false}""", classOf[AnnotatedOptionBoolean])
    v1 shouldBe AnnotatedOptionBoolean(Some(false))
    v1.valueBoolean.get shouldBe false
    useOptionBoolean(v1.valueBoolean) shouldBe "false"
  }

  it should "deserialize AnnotatedOptionPrimitiveBoolean" in {
    val v1 = deserialize("""{"valueBoolean":false}""", classOf[AnnotatedOptionPrimitiveBoolean])
    v1 shouldBe AnnotatedOptionPrimitiveBoolean(Some(false))
    v1.valueBoolean.get shouldBe false
    useOptionBoolean(v1.valueBoolean) shouldBe "false"
  }

  it should "deserialize OptionBoolean (without registerReferencedValueType)" in {
    val v1 = deserialize("""{"valueBoolean":false}""", classOf[OptionBoolean])
    v1 shouldBe OptionBoolean(Some(false))
    v1.valueBoolean.get shouldBe false
    useOptionBoolean(v1.valueBoolean) shouldBe "false"
  }

  it should "deserialize OptionBoolean (with registerReferencedValueType)" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionBoolean], "valueBoolean", classOf[Boolean])
    try {
      val v1 = deserialize("""{"valueBoolean":false}""", classOf[OptionBoolean])
      v1 shouldBe OptionBoolean(Some(false))
      v1.valueBoolean.get shouldBe false
      useOptionBoolean(v1.valueBoolean) shouldBe "false"
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize OptionJavaBoolean" in {
    val v1 = deserialize("""{"valueBoolean":false}""", classOf[OptionJavaBoolean])
    v1 shouldBe OptionJavaBoolean(Some(false))
    v1.valueBoolean.get shouldBe false
    useOptionJavaBoolean(v1.valueBoolean) shouldBe "false"
  }
}
