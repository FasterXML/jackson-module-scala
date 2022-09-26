package tools.jackson.module.scala.deser

import tools.jackson.databind.DeserializationFeature
import tools.jackson.datatype.guava.GuavaModule
import tools.jackson.module.scala.{BaseSpec, DefaultScalaModule}
import com.google.common.collect.Multimap
import tools.jackson.databind.json.JsonMapper

class PojoWithMultiMap(val headers: Multimap[String, String])

class GuavaModuleTest extends BaseSpec {
  "Scala module" should "work with GuavaModule (Scala registered second)" in {
    val builder = JsonMapper.builder().addModules(new GuavaModule, DefaultScalaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap: PojoWithMultiMap = objectReader.forType(classOf[PojoWithMultiMap])
      .readValue(objectReader.treeAsTokens(objectReader.readTree(source)))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with GuavaModule (Scala registered first)" in {
    val builder = JsonMapper.builder().addModules(DefaultScalaModule, new GuavaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source: String = "{\"headers\":{\"key1\": [\"value1\"] }}"

    val pojoWithMultiMap: PojoWithMultiMap = objectReader.forType(classOf[PojoWithMultiMap])
      .readValue(objectReader.treeAsTokens(objectReader.readTree(source)))
    Option(pojoWithMultiMap) shouldBe defined
  }

  "Scala module" should "work with Guava MultiMap" in {
    val builder = JsonMapper.builder().addModules(DefaultScalaModule, new GuavaModule)
    val objectMapper = builder.build()
    val objectReader = objectMapper.reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val source = "{\"key1\": [\"value1\"] }"

    val mmType = objectMapper.getTypeFactory.constructMapLikeType(
      classOf[com.google.common.collect.Multimap[String, String]], classOf[String], classOf[String])
    val multiMap: com.google.common.collect.Multimap[String, String] = objectReader.forType(mmType)
      .readValue(objectReader.treeAsTokens(objectReader.readTree(source)))
    Option(multiMap) shouldBe defined
  }
}
