package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonProperty

import com.google.common.collect.Multimap

import org.junit.Test
import org.junit.Assert.assertNotNull
import com.fasterxml.jackson.databind.{ObjectReader, DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class PojoWithMultiMap(val headers: Multimap[String, String])

class GuavaModuleTest {

  @Test
  def testScalaIsSecond() = {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new GuavaModule)
    objectMapper.registerModule(new DefaultScalaModule)
    val objectReader = objectMapper.reader.asInstanceOf[ObjectReader].without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    assertNotNull(pojoWithMultiMap)
  }
  @Test
  def testScalaIsFirst() = {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new DefaultScalaModule)
    objectMapper.registerModule(new GuavaModule)
    val objectReader = objectMapper.reader.asInstanceOf[ObjectReader].without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap : PojoWithMultiMap = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructType(classOf[PojoWithMultiMap]))
    assertNotNull(pojoWithMultiMap)
  }
  @Test
  def testNotPropertyBased() = {
    val objectMapper = new ObjectMapper
    objectMapper.registerModule(new DefaultScalaModule)
    objectMapper.registerModule(new GuavaModule)
    val objectReader = objectMapper.reader.asInstanceOf[ObjectReader].without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"key1\": [\"value1\"] }"

    val multiMap : com.google.common.collect.Multimap[String,String] = objectReader.readValue(objectReader.treeAsTokens(objectReader.readTree(source)),
      objectMapper.getTypeFactory.constructMapLikeType(classOf[com.google.common.collect.Multimap[String,String]],classOf[String],classOf[String]))
    assertNotNull(multiMap)
  }

}
