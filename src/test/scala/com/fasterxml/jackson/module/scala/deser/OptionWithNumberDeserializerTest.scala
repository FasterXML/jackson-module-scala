package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object OptionWithNumberDeserializerTest {
  case class AnnotatedOptionLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) valueLong: Option[Long])
  case class AnnotatedOptionPrimitiveLong(@JsonDeserialize(contentAs = classOf[Long]) valueLong: Option[Long])
  case class OptionLong(valueLong: Option[Long])
  case class OptionLongWithDefault(valueLong: Option[Long] = None)
  case class OptionJavaLong(valueLong: Option[java.lang.Long])
  case class OptionBigInt(value: Option[BigInt])
  case class WrappedOptionLong(text: String, wrappedLong: OptionLong)
}

class OptionWithNumberDeserializerTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import OptionWithNumberDeserializerTest._

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)
  private def useOptionJavaLong(v: Option[java.lang.Long]): Long = v.map(_ * 2).getOrElse(0L)
  private def useOptionBigInt(v: Option[BigInt]): Long = v.map(_ * 2).map(_.toLong).getOrElse(0L)

  "JacksonModuleScala" should "deserialize AnnotatedOptionLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[AnnotatedOptionLong])
    v1 shouldBe AnnotatedOptionLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionLong(v1.valueLong) shouldBe 302L
  }

  it should "deserialize AnnotatedOptionPrimitiveLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[AnnotatedOptionPrimitiveLong])
    v1 shouldBe AnnotatedOptionPrimitiveLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionLong(v1.valueLong) shouldBe 302L
  }

  it should "deserialize OptionLong when registerReferencedValueType is used" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    try {
      val v1 = deserialize("""{"valueLong":151}""", classOf[OptionLong])
      v1 shouldBe OptionLong(Some(151L))
      v1.valueLong.get shouldBe 151L
      //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in OptionWithNumberDeserializerTest
      useOptionLong(v1.valueLong) shouldBe 302L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize OptionLongWithDefault when registerReferencedValueType is used" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLongWithDefault], "valueLong", classOf[Long])
    try {
      val v1 = deserialize("""{"valueLong":151}""", classOf[OptionLongWithDefault])
      v1 shouldBe OptionLongWithDefault(Some(151L))
      v1.valueLong.get shouldBe 151L
      //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in OptionWithNumberDeserializerTest
      useOptionLong(v1.valueLong) shouldBe 302L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize WrappedOptionLong when registerReferencedValueType is used" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[OptionLong], "valueLong", classOf[Long])
    try {
      val v1 = deserialize("""{"text":"myText","wrappedLong":{"valueLong":151}}""", classOf[WrappedOptionLong])
      v1 shouldBe WrappedOptionLong("myText", OptionLong(Some(151L)))
      v1.wrappedLong.valueLong.get shouldBe 151L
      //this next call will fail with a Scala unboxing exception unless you call ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in OptionWithNumberDeserializerTest
      useOptionLong(v1.wrappedLong.valueLong) shouldBe 302L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "fail to deserialize OptionLong when value is text" in {
    intercept[InvalidFormatException] {
      deserialize("""{"valueLong":"xy"}""", classOf[OptionLong])
    }.getMessage should include("""Cannot deserialize value of type `long` from String "xy": not a valid `long` value""")
  }

  it should "deserialize OptionJavaLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[OptionJavaLong])
    v1 shouldBe OptionJavaLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionJavaLong(v1.valueLong) shouldBe 302L
  }

  it should "deserialize OptionBigInt" in {
    val v1 = deserialize("""{"value":151}""", classOf[OptionBigInt])
    v1 shouldBe OptionBigInt(Some(BigInt(151L)))
    v1.value.get shouldBe 151L
    useOptionBigInt(v1.value) shouldBe 302L
  }
}
