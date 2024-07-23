package tools.jackson.module.scala.deser

import tools.jackson.module.scala.{DefaultScalaModule, JacksonModule}

import scala.util.Properties.versionNumberString

object AnyValDeserializerTest {
  case class DoubleAnyVal(underlying: Double) extends AnyVal
  case class DoubleAnyValHolder(value: DoubleAnyVal)

  case class BigIntAnyVal(underlying: BigInt) extends AnyVal
  case class BigIntAnyValHolder(value: BigIntAnyVal)
  case class BigIntOptionAnyValHolder(value: Option[BigIntAnyVal])
}

class AnyValDeserializerTest extends DeserializerTest {
  import AnyValDeserializerTest._

  lazy val module: JacksonModule = DefaultScalaModule

  behavior of "AnyVal"

  it should "deserialize an Double AnyVal" in {
    val mapper = newMapper
    val expected = DoubleAnyVal(42)
    mapper.readValue("""{"underlying":42.0}""", classOf[DoubleAnyVal]) shouldEqual expected
    mapper.readValue("""{"value":42.0}""", classOf[DoubleAnyValHolder]) shouldEqual DoubleAnyValHolder(expected)
  }

  it should "deserialize an BigInt AnyVal" in {
    val mapper = newMapper
    val expected = BigIntAnyVal(42)
    mapper.readValue("""{"underlying":42}""", classOf[BigIntAnyVal]) shouldEqual expected
    mapper.readValue("""{"value":42}""", classOf[BigIntAnyValHolder]) shouldEqual BigIntAnyValHolder(expected)
    if (!versionNumberString.startsWith("2.11")) {
      // see https://github.com/FasterXML/jackson-module-scala/pull/675
      mapper.readValue("""{"value":{"underlying":42}}""", classOf[BigIntOptionAnyValHolder]) shouldEqual
        BigIntOptionAnyValHolder(Some(expected))
    }
  }
}
