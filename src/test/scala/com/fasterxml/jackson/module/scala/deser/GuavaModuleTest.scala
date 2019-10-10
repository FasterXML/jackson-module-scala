package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.collect.Multimap
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner

class PojoWithMultiMap(val headers: Multimap[String, String])

@RunWith(classOf[JUnitRunner])
class GuavaModuleTest extends FlatSpec with Matchers {
  "Scala module" should "work with GuavaModule (Scala registered second)" in {
    val builder = JsonMapper.builder().addModules(new GuavaModule, new DefaultScalaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with GuavaModule (Scala registered first)" in {
    val builder = JsonMapper.builder().addModules(new DefaultScalaModule, new GuavaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with Guava MultiMap" in {
    val builder = JsonMapper.builder().addModules(new DefaultScalaModule, new GuavaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"key1\": [\"value1\"] }"

    val multiMap : com.google.common.collect.Multimap[String,String] = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructMapLikeType(classOf[com.google.common.collect.Multimap[String,String]],classOf[String],classOf[String]))
    Option(multiMap) shouldBe defined
  }
}
