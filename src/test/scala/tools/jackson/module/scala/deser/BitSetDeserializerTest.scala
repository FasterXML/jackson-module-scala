package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.{BitSetDeserializerModule, DefaultScalaModule, JacksonModule}

import java.nio.charset.StandardCharsets
import scala.collection.{immutable, mutable}

class BitSetDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule
  val arraySize = 100
  val obj = immutable.BitSet(0 until arraySize: _*)
  val jsonString = obj.mkString("[", ",", "]")
  val jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8)

  "An ObjectMapper with the DefaultScalaModule" should "not handle immutable BitSet" in {
    val mapper = newMapper
    intercept[ClassCastException] {
      mapper.readValue(jsonBytes, new TypeReference[immutable.BitSet] {})
    }
  }

  it should "not handle mutable BitSet" in {
    val mapper = newMapper
    intercept[ClassCastException] {
      mapper.readValue(jsonBytes, new TypeReference[mutable.BitSet] {})
    }
  }

  it should "handle BitSet when type is specified as HashSet" in {
    val mapper = newMapper
    val seq = mapper.readValue(jsonBytes, new TypeReference[immutable.HashSet[Int]] {})
    seq should have size arraySize
  }

  it should "handle BitSet when type is specified as mutable HashSet" in {
    val mapper = newMapper
    val seq = mapper.readValue(jsonBytes, new TypeReference[mutable.HashSet[Int]] {})
    seq should have size arraySize
  }

  "An ObjectMapper with the BitSetDeserializerModule" should "handle immutable BitSet" in {
    val mapper = JsonMapper.builder()
      .addModule(BitSetDeserializerModule)
      .addModule(DefaultScalaModule)
      .build()
    val seq = mapper.readValue(jsonBytes, new TypeReference[immutable.BitSet] {})
    seq should have size arraySize
  }

  it should "handle mutable BitSet" in {
    val mapper = JsonMapper.builder()
      .addModule(BitSetDeserializerModule)
      .addModule(DefaultScalaModule)
      .build()
    val seq = mapper.readValue(jsonBytes, new TypeReference[mutable.BitSet] {})
    seq should have size arraySize
  }
}
