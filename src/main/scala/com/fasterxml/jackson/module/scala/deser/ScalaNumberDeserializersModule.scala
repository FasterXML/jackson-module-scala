package com.fasterxml.jackson
package module.scala
package deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.{
  BigDecimalDeserializer => JavaBigDecimalDeserializer, BigIntegerDeserializer
}

private object BigDecimalDeserializer extends StdScalarDeserializer[BigDecimal](classOf[BigDecimal]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    JavaBigDecimalDeserializer.instance.deserialize(p, ctxt)
  }
}

private object BigIntDeserializer extends StdScalarDeserializer[BigInt](classOf[BigInt]) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): BigInt = {
    BigIntegerDeserializer.instance.deserialize(p, ctxt)
  }
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
  this += NumberDeserializers
}
