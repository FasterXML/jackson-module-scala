package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.collection.immutable

class Map2DeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule

  "An ObjectMapper with the SortedMapDeserializerModule" should "deserialize an object into an TreeSeqMap" in {
    val result = deserialize(mapJson, new TypeReference[immutable.TreeSeqMap[String,String]]{})
    result should equal (mapScala)
  }

  private val mapJson =  """{ "one": "1", "two": "2" }"""
  private val mapScala = collection.SortedMap("one"->"1","two"->"2")
}
