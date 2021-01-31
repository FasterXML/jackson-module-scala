package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object ParamWithDashNameDeserializerTest {
  case class AnnotatedOptionLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) valueLong: Option[Long])

  case class OptionLongWithDash(`value-long`: Option[Long])

  case class AnnotatedOptionLongWithDash(@JsonDeserialize(contentAs = classOf[java.lang.Long]) `value-long`: Option[Long])

  case class AnnotatedOptionLongWithDashButChangeToCamelCase(@JsonProperty("value-long") @JsonDeserialize(contentAs = classOf[java.lang.Long]) valueLong: Option[Long])
}

class ParamWithDashNameDeserializerTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import ParamWithDashNameDeserializerTest._

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)

  "JacksonModuleScala" should "support standard param names" in {
    // check deserialization
    val v1 = deserialize("""{"valueLong":151}""", classOf[AnnotatedOptionLong])
    v1 shouldBe AnnotatedOptionLong(Some(151L))
    v1.valueLong.get shouldBe 151L

    val v2 = deserialize(serialize(AnnotatedOptionLong(Some(152))), classOf[AnnotatedOptionLong])
    v2 shouldBe AnnotatedOptionLong(Some(152L))
    v2.valueLong.get shouldBe 152L
    useOptionLong(v2.valueLong) shouldBe 304L
  }

  it should "support param names with dashes" in {
    // check deserialization
    val typeRef = new TypeReference[OptionLongWithDash] {}
    val v1 = deserialize("""{"value-long":251}""", typeRef)
    v1 shouldBe OptionLongWithDash(Some(251L))
    v1.`value-long`.get shouldBe 251L

    // serialize from case class then deserialize and then apply the method that will fail
    val v2 = deserialize(serialize(OptionLongWithDash(Some(252))), typeRef)
    v2 shouldBe OptionLongWithDash(Some(252L))
    v2.`value-long`.get shouldBe 252L
    //next assert fails due to unboxing issue -- without the @JsonDeserialize to help, jackson will
    //erroneously create an Option[Int] instead of Option[Long] leading to a class cast exception
    //useOptionLong(v2.`value-long`) shouldBe 504L
  }

  it should "support param names with dashes (annotated case)" in {
    // check deserialization
    val typeRef = new TypeReference[AnnotatedOptionLongWithDash] {}
    val v1 = deserialize("""{"value-long":251}""", typeRef)
    v1 shouldBe AnnotatedOptionLongWithDash(Some(251L))
    v1.`value-long`.get shouldBe 251L

    val v2 = deserialize(serialize(AnnotatedOptionLongWithDash(Some(252))), typeRef)
    v2 shouldBe AnnotatedOptionLongWithDash(Some(252L))
    v2.`value-long`.get shouldBe 252L
    useOptionLong(v2.`value-long`) shouldBe 504L
  }

  it should "support renaming param names to names with dashes" in {
    // check deserialization
    val typeRef = new TypeReference[AnnotatedOptionLongWithDashButChangeToCamelCase] {}
    val v1 = deserialize("""{"value-long":351}""", typeRef)
    v1 shouldBe AnnotatedOptionLongWithDashButChangeToCamelCase(Some(351L))
    v1.valueLong.get shouldBe 351L

    val v2 = deserialize(serialize(AnnotatedOptionLongWithDashButChangeToCamelCase(Some(352))), typeRef)
    v2 shouldBe AnnotatedOptionLongWithDashButChangeToCamelCase(Some(352L))
    v2.valueLong.get shouldBe 352L
    useOptionLong(v2.valueLong) shouldBe 704L
  }
}
