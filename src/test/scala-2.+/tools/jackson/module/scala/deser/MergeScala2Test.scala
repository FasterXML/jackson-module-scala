package tools.jackson.module.scala.deser

import com.fasterxml.jackson.annotation.JsonMerge
import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.{MapperFeature, ObjectMapper, ObjectReader}
import tools.jackson.module.scala.DefaultScalaModule

import scala.collection.{Map, mutable}

class MergeScala2Test extends DeserializerTest {

  val module: DefaultScalaModule.type = DefaultScalaModule

  // This test relies on enabling MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS
  // which is not enabled by default in the Jackson v2 but not in Jackson v3
  def newScalaMapper: ObjectMapper = newBuilder
    .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
    .build()


  val firstPairMapJson = """{"one":{"first":"1"},"two":{"second":"2"},"three":{"first":"3","second":"4"}}"""
  val secondPairMapJson = """{"two":{"first":"22"},"three":{"second":"33"}}"""
  val secondPairMap = Map("two" -> Pair("22", null), "three" -> Pair(null, "33"))
  val mergedPairMap = Map("one" -> Pair("1", null), "two" -> Pair("22", "2"), "three" -> Pair("3", "33"))

  behavior of "The DefaultScalaModule when reading for updating"

  // started failing in jackson-module-scala v3.2.0 but only for Scala 3
  // see https://github.com/FasterXML/jackson-module-scala/issues/817
  it should "merge only the annotated pair map" in {
    val typeReference = new TypeReference[ClassWithMaps[Pair]]{}
    val initial = deserialize(classJson(firstPairMapJson), typeReference)
    val result = updateValue(newScalaMapper, initial, typeReference, classJson(secondPairMapJson))

    result shouldBe ClassWithMaps(secondPairMap, mergedPairMap)
  }

  def classJson(nestedJson: String) = s"""{"field1":$nestedJson,"field2":$nestedJson}"""

  private def updateValue[T](mapper: ObjectMapper, valueToUpdate: T,
                             typeReference: TypeReference[T], src: String): T = {
    objectReaderFor(mapper, valueToUpdate, typeReference).readValue(src)
  }

  private def objectReaderFor[T](mapper: ObjectMapper, valueToUpdate: T,
                                 typeReference: TypeReference[T]): ObjectReader = {
    mapper.readerForUpdating(valueToUpdate).forType(typeReference)
  }
}
