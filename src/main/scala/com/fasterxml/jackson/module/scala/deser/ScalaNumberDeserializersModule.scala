package __foursquare_shaded__.com.fasterxml.jackson
package module.scala
package deser

import __foursquare_shaded__.com.fasterxml.jackson.core.JsonToken.{START_ARRAY, VALUE_NUMBER_FLOAT, VALUE_NUMBER_INT, VALUE_STRING}
import __foursquare_shaded__.com.fasterxml.jackson.core.{JsonParser, JsonToken}
import __foursquare_shaded__.com.fasterxml.jackson.databind.DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS
import __foursquare_shaded__.com.fasterxml.jackson.databind.deser.Deserializers
import __foursquare_shaded__.com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import __foursquare_shaded__.com.fasterxml.jackson.databind._

import scala.reflect.{ClassTag, classTag}

private abstract class BigNumberDeserializer[T >: Null : ClassTag](creator: (String) => T)
  extends StdScalarDeserializer[T](classTag[T].runtimeClass)
{
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): T = {
    val t = jp.getCurrentToken
    t match {
      case VALUE_NUMBER_INT | VALUE_NUMBER_FLOAT => creator(jp.getText.trim)
      case VALUE_STRING =>
        val text = jp.getText.trim
        if (text.isEmpty) null else try {
          creator(text)
        }
        catch {
          case e: IllegalArgumentException => throw ctxt.weirdStringException(text, _valueClass, "not a valid representation")
        }
      case START_ARRAY if ctxt.isEnabled(UNWRAP_SINGLE_VALUE_ARRAYS) =>
        jp.nextToken()
        val value = deserialize(jp, ctxt)
        if (jp.nextToken() != JsonToken.END_ARRAY) {
          throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, "Attempted to unwrap array for single value but there was more than a single value in the array")
        }
        value
      case _ =>
        ctxt.handleUnexpectedToken(_valueClass, jp).asInstanceOf[T]
    }
  }
}

private object BigDecimalDeserializer extends BigNumberDeserializer(BigDecimal.apply)

private object BigIntDeserializer extends BigNumberDeserializer(BigInt.apply)

private object NumberDeserializers extends Deserializers.Base
{
  val BigDecimalClass = BigDecimalDeserializer.handledType()
  val BigIntClass = BigIntDeserializer.handledType()

  override def findBeanDeserializer(tpe: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] =
    tpe.getRawClass match {
      case BigDecimalClass => BigDecimalDeserializer
      case BigIntClass => BigIntDeserializer
      case _ => null
    }
}

trait ScalaNumberDeserializersModule extends JacksonModule {
  this += NumberDeserializers
}
