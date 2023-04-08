package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.deser.Issue626DeserializerTest._
import com.fasterxml.jackson.module.scala.{BaseSpec, DefaultScalaModule}

object Issue626DeserializerTest {
  case class I(i: Int)
  case class W(b: List[I])

  case class OptionI(i: Option[Int])

  case class OptionW(b: List[OptionI])
}

class Issue626DeserializerTest extends BaseSpec {

  "Jackson Scala Module" should "deserialize a W" in {
    val mapper: JsonMapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
      .build()
    val d = mapper.readValue(
      """{ "b": [ { "i": 1 }, { "i": 2 } ] }""", classOf[W])
    d.b shouldEqual List(I(1), I(2))
  }
  it should "deserialize an OptionW" in {
    val mapper: JsonMapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
      .build()
    val d = mapper.readValue(
      """{ "b": [ { "i": 1 }, { "i": 2 } ] }""", classOf[OptionW])
    d.b shouldEqual List(OptionI(Some(1)), OptionI(Some(2)))
  }
}
