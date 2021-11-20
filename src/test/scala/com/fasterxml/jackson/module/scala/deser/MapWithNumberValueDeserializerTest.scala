package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospectorModule

object MapWithNumberValueDeserializerTest {
  case class AnnotatedMapLong(@JsonDeserialize(contentAs = classOf[java.lang.Long]) longs: Map[String, Long])
  case class AnnotatedMapPrimitiveLong(@JsonDeserialize(contentAs = classOf[Long]) longs: Map[String, Long])
  case class MapLong(longs: Map[String, Long])
  case class MapJavaLong(longs: Map[String, java.lang.Long])
  case class MapBigInt(longs: Map[String, BigInt])
}

class MapWithNumberValueDeserializerTest extends DeserializerTest {
  lazy val module: DefaultScalaModule.type = DefaultScalaModule
  import MapWithNumberValueDeserializerTest._

  private def sumMapLong(m: Map[String, Long]): Long = m.values.sum
  private def sumMapJavaLong(m: Map[String, java.lang.Long]): Long = m.values.map(_.toLong).sum
  private def sumMapBigInt(m: Map[String, BigInt]): Long = m.values.sum.toLong

  "JacksonModuleScala" should "deserialize AnnotatedMapLong" in {
    val v1 = deserialize("""{"longs":{"151":151,"152":152,"153":153}}""", classOf[AnnotatedMapLong])
    v1 shouldBe AnnotatedMapLong(Map("151" -> 151L, "152" -> 152L, "153" -> 153L))
    sumMapLong(v1.longs) shouldBe 456L
  }

  it should "deserialize AnnotatedMapPrimitiveLong" in {
    val v1 = deserialize("""{"longs":{"151":151,"152":152,"153":153}}""", classOf[AnnotatedMapPrimitiveLong])
    v1 shouldBe AnnotatedMapPrimitiveLong(Map("151" -> 151L, "152" -> 152L, "153" -> 153L))
    sumMapLong(v1.longs) shouldBe 456L
  }

  it should "deserialize MapLong" in {
    ScalaAnnotationIntrospectorModule.registerReferencedValueType(classOf[MapLong], "longs", classOf[Long])
    try {
      val v1 = deserialize("""{"longs":{"151":151,"152":152,"153":153}}""", classOf[MapLong])
      v1 shouldBe MapLong(Map("151" -> 151L, "152" -> 152L, "153" -> 153L))
      //this will next call will fail with a Scala unboxing exception unless you ScalaAnnotationIntrospectorModule.registerReferencedValueType
      //or use one of the equivalent classes in MapWithNumberDeserializerTest
      sumMapLong(v1.longs) shouldBe 456L
    } finally {
      ScalaAnnotationIntrospectorModule.clearRegisteredReferencedTypes()
    }
  }

  it should "deserialize MapJavaLong" in {
    val v1 = deserialize("""{"longs":{"151":151,"152":152,"153":153}}""", classOf[MapJavaLong])
    v1 shouldBe MapJavaLong(Map("151" -> 151L, "152" -> 152L, "153" -> 153L))
    sumMapJavaLong(v1.longs) shouldBe 456L
  }

  it should "deserialize MapBigInt" in {
    val v1 = deserialize("""{"longs":{"151":151,"152":152,"153":153}}""", classOf[MapBigInt])
    v1 shouldBe MapBigInt(Map("151" -> 151L, "152" -> 152L, "153" -> 153L))
    sumMapBigInt(v1.longs) shouldBe 456L
  }
}
