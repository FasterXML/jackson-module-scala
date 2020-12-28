package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.module.scala.{BaseSpec, DefaultScalaModule}
import com.google.common.collect.Multimap

class PojoWithMultiMap(val headers: Multimap[String, String])

class GuavaModuleTest extends BaseSpec {
  "Scala module" should "work with GuavaModule (Scala registered second)" in {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new GuavaModule)
    objectMapper.registerModule(new DefaultScalaModule)
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with GuavaModule (Scala registered first)" in {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new DefaultScalaModule)
    objectMapper.registerModule(new GuavaModule)
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with Guava MultiMap" in {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new DefaultScalaModule)
    objectMapper.registerModule(new GuavaModule)
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"key1\": [\"value1\"] }"

    val multiMap : com.google.common.collect.Multimap[String,String] = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructMapLikeType(classOf[com.google.common.collect.Multimap[String,String]],classOf[String],classOf[String]))
    Option(multiMap) shouldBe defined
  }
}
