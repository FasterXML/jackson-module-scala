package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule
import org.scalatest.BeforeAndAfterEach

object SeqWithNumberDeserializerTest {
  case class AnnotatedSeqLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) longs: Seq[Long])
  case class AnnotatedSeqPrimitiveLong(@JsonDeserialize(contentAs = classOf[Long]) longs: Seq[Long])
  case class SeqLong(longs: Seq[Long])
  case class SeqJavaLong(longs: Seq[java.lang.Long])
  case class SeqBigInt(longs: Seq[BigInt])
}

class SeqWithNumberDeserializerTest extends DeserializerTest with BeforeAndAfterEach {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import SeqWithNumberDeserializerTest._

  private def sumSeqLong(v: Seq[Long]): Long = v.sum
  private def sumSeqJavaLong(v: Seq[java.lang.Long]): Long = v.map(_.toLong).sum
  private def sumSeqBigInt(v: Seq[BigInt]): Long = v.sum.toLong

  "JacksonModuleScala" should "deserialize AnnotatedSeqLong" in {
    val v1 = deserialize("""{"longs":[151,152,153]}""", classOf[AnnotatedSeqLong])
    v1 shouldBe AnnotatedSeqLong(Seq(151L, 152L, 153L))
    sumSeqLong(v1.longs) shouldBe 456L
  }

  it should "deserialize AnnotatedSeqPrimitiveLong" in {
    val v1 = deserialize("""{"longs":[151,152,153]}""", classOf[AnnotatedSeqPrimitiveLong])
    v1 shouldBe AnnotatedSeqPrimitiveLong(Seq(151L, 152L, 153L))
    sumSeqLong(v1.longs) shouldBe 456L
  }

  it should "deserialize SeqLong" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[SeqLong], "longs", classOf[Long])
    try {
      val v1 = deserialize("""{"longs":[151,152,153]}""", classOf[SeqLong])
      v1 shouldBe SeqLong(Seq(151L, 152L, 153L))
      //this will next call will fail with a Scala unboxing exception unless you ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in SeqWithNumberDeserializerTest
      sumSeqLong(v1.longs) shouldBe 456L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize SeqJavaLong" in {
    val v1 = deserialize("""{"longs":[151,152,153]}""", classOf[SeqJavaLong])
    v1 shouldBe SeqJavaLong(Seq(151L, 152L, 153L))
    sumSeqJavaLong(v1.longs) shouldBe 456L
  }

  it should "deserialize SeqBigInt" in {
    val v1 = deserialize("""{"longs":[151,152,153]}""", classOf[SeqBigInt])
    v1 shouldBe SeqBigInt(Seq(151L, 152L, 153L))
    sumSeqBigInt(v1.longs) shouldBe 456L
  }
}
