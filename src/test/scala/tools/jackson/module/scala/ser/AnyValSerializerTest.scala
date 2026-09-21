package tools.jackson.module.scala.ser

import tools.jackson.module.scala.BaseFixture

object AnyValSerializerTest {
  case class DoubleAnyVal(underlying: Double) extends AnyVal
  case class DoubleAnyValHolder(value: DoubleAnyVal)

  case class BigIntAnyVal(underlying: BigInt) extends AnyVal
  case class BigIntAnyValHolder(value: BigIntAnyVal)
  case class BigIntOptionAnyValHolder(value: Option[BigIntAnyVal])

  case class TypedLabel[T](value: T) extends AnyVal
  case class TypedLabels(one: TypedLabel[String], many: List[TypedLabel[Int]])
}

class AnyValSerializerTest extends BaseFixture {
  import AnyValSerializerTest._

  behavior of "AnyVal"

  it should "serialize an Double AnyVal" in { mapper =>
    val value = DoubleAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42.0}"""
    mapper.writeValueAsString(DoubleAnyValHolder(value)) shouldBe """{"value":42.0}"""
  }

  it should "serialize an BigInt AnyVal" in { mapper =>
    val value = BigIntAnyVal(42)
    mapper.writeValueAsString(value) shouldBe """{"underlying":42}"""
    mapper.writeValueAsString(BigIntAnyValHolder(value)) shouldBe """{"value":42}"""
    // see https://github.com/FasterXML/jackson-module-scala/pull/675
    mapper.writeValueAsString(BigIntOptionAnyValHolder(Some(value))) shouldBe """{"value":{"underlying":42}}"""
  }

  it should "serialize a generic AnyVal as its underlying type in a field and boxed in a collection" in { mapper =>
    // the field is erased to String, while each List element is a boxed TypedLabel instance
    val value = TypedLabels(TypedLabel("x"), List(TypedLabel(1), TypedLabel(2)))
    mapper.writeValueAsString(value) shouldBe """{"one":"x","many":[{"value":1},{"value":2}]}"""
  }

}
