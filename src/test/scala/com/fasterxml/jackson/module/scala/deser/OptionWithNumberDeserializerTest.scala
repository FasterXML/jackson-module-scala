package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector
import org.scalatest.BeforeAndAfterEach

object OptionWithNumberDeserializerTest {
  case class AnnotatedOptionLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) valueLong: Option[Long])
  case class AnnotatedOptionPrimitiveLong(@JsonDeserialize(contentAs = classOf[Long]) valueLong: Option[Long])
  case class OptionLong(valueLong: Option[Long])
  case class OptionJavaLong(valueLong: Option[java.lang.Long])
  case class OptionBigInt(value: Option[BigInt])
}

class OptionWithNumberDeserializerTest extends DeserializerTest with BeforeAndAfterEach {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import OptionWithNumberDeserializerTest._

  private def useOptionLong(v: Option[Long]): Long = v.map(_ * 2).getOrElse(0L)
  private def useOptionJavaLong(v: Option[java.lang.Long]): Long = v.map(_ * 2).getOrElse(0L)
  private def useOptionBigInt(v: Option[BigInt]): Long = v.map(_ * 2).map(_.toLong).getOrElse(0L)

  override def afterEach(): Unit = {
    super.afterEach()
    ScalaAnnotationIntrospector.clearRegisteredReferencedTypes()
  }

  "JacksonModuleScala" should "support AnnotatedOptionLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[AnnotatedOptionLong])
    v1 shouldBe AnnotatedOptionLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionLong(v1.valueLong) shouldBe 302L
  }

  it should "support AnnotatedOptionPrimitiveLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[AnnotatedOptionPrimitiveLong])
    v1 shouldBe AnnotatedOptionPrimitiveLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionLong(v1.valueLong) shouldBe 302L
  }

  it should "support OptionLong" in {
    ScalaAnnotationIntrospector.registerReferencedType(classOf[OptionLong], "valueLong", classOf[Long])
    val v1 = deserialize("""{"valueLong":151}""", classOf[OptionLong])
    v1 shouldBe OptionLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    //this will next call will fail with a Scala unboxing exception unless you BeanIntrospector.registerReferencedType
    //or use one of the equivalent classes in OptionWithNumberDeserializerTest
    useOptionLong(v1.valueLong) shouldBe 302L
  }

  it should "support OptionJavaLong" in {
    val v1 = deserialize("""{"valueLong":151}""", classOf[OptionJavaLong])
    v1 shouldBe OptionJavaLong(Some(151L))
    v1.valueLong.get shouldBe 151L
    useOptionJavaLong(v1.valueLong) shouldBe 302L
  }

  it should "support OptionBigInt" in {
    val v1 = deserialize("""{"value":151}""", classOf[OptionBigInt])
    v1 shouldBe OptionBigInt(Some(BigInt(151L)))
    v1.value.get shouldBe 151L
    useOptionBigInt(v1.value) shouldBe 302L
  }
}
