package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.databind.deser.{ValueInstantiator, ValueInstantiators, Deserializers}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator

class BigDecimalInstantiator(config: DeserializationConfig) extends StdValueInstantiator(config, classOf[BigDecimal]) {

  override def canCreateFromInt = true
  override def createFromInt(ctx: DeserializationContext, value: Int) = BigDecimal(value)
  override def canCreateFromLong = true
  override def createFromLong(ctx: DeserializationContext, value: Long) = BigDecimal(value)
  override def canCreateFromDouble = true
  override def createFromDouble(ctx: DeserializationContext, value: Double) = BigDecimal(value)
  override def canCreateFromString = true
  override def createFromString(ctx: DeserializationContext, value: String) = BigDecimal(value)
}

object ScalaStdValueInstatiators extends ValueInstantiators {
  val BIG_DECIMAL = classOf[BigDecimal]

  override def findValueInstantiator(config: DeserializationConfig, beanDesc: BeanDescription, defaultInstantiator: ValueInstantiator): ValueInstantiator = {
    if (BIG_DECIMAL.isAssignableFrom(beanDesc.getBeanClass))
      new BigDecimalInstantiator(config)
    else
      defaultInstantiator
  }

}

trait ScalaStdValueInstantiatorsModule extends JacksonModule {
  this += (_.addValueInstantiators(ScalaStdValueInstatiators))
}
