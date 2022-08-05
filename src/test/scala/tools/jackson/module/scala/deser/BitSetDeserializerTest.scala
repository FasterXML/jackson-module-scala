package tools.jackson.module.scala.deser

import tools.jackson.core.`type`.TypeReference
import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import java.nio.charset.StandardCharsets
import scala.collection.{immutable, mutable}

class BitSetDeserializerTest extends DeserializerTest {

  lazy val module: JacksonModule = DefaultScalaModule
  val arraySize = 100
  val obj = immutable.BitSet(0 until arraySize: _*)
  val jsonString = obj.mkString("[", ",", "]")
  val jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8)

  "An ObjectMapper with the SeqDeserializer" should "handle immutable BitSet" in {
    val mapper = newMapper
    val seq = mapper.readValue(jsonBytes, new TypeReference[immutable.BitSet] {})
    seq should have size arraySize
  }

  it should "handle mutable BitSet" in {
    val mapper = newMapper
    val seq = mapper.readValue(jsonBytes, new TypeReference[mutable.BitSet] {})
    seq should have size arraySize
  }
}
