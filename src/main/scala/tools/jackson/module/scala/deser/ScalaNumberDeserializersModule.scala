package tools.jackson
package module.scala
package deser

import tools.jackson.core.JsonParser
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.deser.std.StdScalarDeserializer
import tools.jackson.databind.deser.jdk.NumberDeserializers.{
  BigDecimalDeserializer => JavaBigDecimalDeserializer,
  BigIntegerDeserializer
}
import tools.jackson.databind._
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule => JacksonScalaModule}

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

private object NumberDeserializers {
  private val BigDecimalClass = classOf[BigDecimal]
  private val BigIntClass = classOf[BigInt]
}

private class NumberDeserializers(config: ScalaModule.Config) extends Deserializers.Base {

  override def findBeanDeserializer(tpe: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): ValueDeserializer[_] =
    tpe.getRawClass match {
      case NumberDeserializers.BigDecimalClass => BigDecimalDeserializer
      case NumberDeserializers.BigIntClass => BigIntDeserializer
      case _ => None.orNull
    }

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    valueType match {
      case NumberDeserializers.BigDecimalClass => true
      case NumberDeserializers.BigIntClass => true
      case _ => false
    }
  }
}

trait ScalaNumberDeserializersModule extends JacksonScalaModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new NumberDeserializers(config)
    builder.build()
  }
}

object ScalaNumberDeserializersModule extends ScalaNumberDeserializersModule
