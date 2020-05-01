package __foursquare_shaded__.com.fasterxml.jackson.module.scala.deser

import __foursquare_shaded__.com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, ObjectReader}
import __foursquare_shaded__.com.fasterxml.jackson.datatype.guava.GuavaModule
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.collect.Multimap
import org.junit.Assert.assertNotNull
import org.junit.Test

class PojoWithMultiMap(val headers: Multimap[String, String])

class GuavaModuleTest {
  @Test
  def testScalaIsSecond(): Unit = {
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
  def testScalaIsFirst(): Unit = {
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
  def testNotPropertyBased(): Unit = {
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
