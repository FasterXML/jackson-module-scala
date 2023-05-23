package com.fasterxml.jackson
package module.scala
package deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.{BigIntegerDeserializer, BigDecimalDeserializer => JavaBigDecimalDeserializer}
import com.fasterxml.jackson.databind._

private object BigDecimalDeserializer extends StdScalarDeserializer[BigDecimal](classOf[BigDecimal]) {
  private val ZERO = BigDecimal(0)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    JavaBigDecimalDeserializer.instance.deserialize(p, ctxt)
  }

  override def getEmptyValue(ctxt: DeserializationContext): AnyRef = ZERO
}

private object BigIntDeserializer extends StdScalarDeserializer[BigInt](classOf[BigInt]) {
  private val ZERO = BigInt(0)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BigInt = {
    BigIntegerDeserializer.instance.deserialize(p, ctxt)
  }

  override def getEmptyValue(ctxt: DeserializationContext): AnyRef = ZERO
}

private object NumberDeserializers extends Deserializers.Base {

  private val BigDecimalClass = classOf[BigDecimal]
  private val BigIntClass = classOf[BigInt]

  override def findBeanDeserializer(tpe: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] =
    tpe.getRawClass match {
      case BigDecimalClass => BigDecimalDeserializer
      case BigIntClass => BigIntDeserializer
      case _ => None.orNull
    }
}

trait ScalaNumberDeserializersModule extends JacksonModule {
  override def getModuleName: String = "ScalaNumberDeserializersModule"
  this += NumberDeserializers
}
