package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.deser.{BeanDeserializerFactory, Deserializers}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}

import scala.languageFeature.postfixOps

private class TupleDeserializer(javaType: JavaType,
                                deserializationConfig: DeserializationConfig,
                                valueDeserializers: Seq[ValueDeserializer[Object]] = Nil,
                                typeDeserializers: Seq[TypeDeserializer] = Nil)
  extends StdDeserializer[Product](classOf[Product]) {

  val cls = javaType.getRawClass
  val ctors = cls.getConstructors
  if (ctors.length != 1) throw new IllegalStateException("Tuple should have exactly one constructor")
  val ctor = ctors.head

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): TupleDeserializer = {
    // For now, the dumb and simple route of assuming we don't have the right deserializers.
    // This will probably result in duplicate deserializers, but it's safer than assuming
    // a current non-empty sequence of valueDeserializers is correct.
    val paramTypes = for (i <- 0 until javaType.containedTypeCount()) yield javaType.containedType(i)

    val paramDesers = paramTypes map (ctxt.findContextualValueDeserializer(_, property))

    val typeDesers: Seq[TypeDeserializer] = {
      if (property != null) {
        paramTypes map (ctxt.findPropertyTypeDeserializer(_, property.getMember))
      } else {
        paramTypes map (ctxt.findTypeDeserializer)
      }
    }

    new TupleDeserializer(javaType, deserializationConfig, paramDesers, typeDesers)
  }


  def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {
    // Ok: must point to START_ARRAY (or equivalent)
    if (jp.isExpectedStartArrayToken) {
      val params = (valueDeserializers zip typeDeserializers) map { case (deser, typeDeser) =>
        jp.nextToken
        if (typeDeser == null)
          deser.deserialize(jp, ctxt)
        else
          deser.deserializeWithType(jp, ctxt, typeDeser)
      }

      val t = jp.nextToken
      if (t != JsonToken.END_ARRAY) {
        ctxt.reportWrongTokenException(ctxt.getContextualType, JsonToken.END_ARRAY,
          "expected closing END_ARRAY after deserialized value")
        // never gets here
        null
      } else {
        ctor.newInstance(params: _*).asInstanceOf[Product]
      }
    } else {
      ctxt.handleUnexpectedToken(javaType, jp).asInstanceOf[Product]
    }
  }
}

private class TupleDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {

  private val PRODUCT = classOf[Product]

  override def findBeanDeserializer(javaType: JavaType,
                                    deserializationConfig: DeserializationConfig,
                                    beanDesc: BeanDescription): ValueDeserializer[_] = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (isOption(cls)) {
      new TupleDeserializer(javaType, deserializationConfig)
    } else {
      super.findBeanDeserializer(javaType, deserializationConfig, beanDesc)
    }
  }

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = isOption(valueType)

  private def isOption(cls: Class[_]): Boolean = {
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    PRODUCT.isAssignableFrom(cls) && cls.getName.startsWith("scala.Tuple")
  }

}

/**
 * Adds deserialization support for Scala Tuples.
 */
trait TupleDeserializerModule extends JacksonModule {
  this += new TupleDeserializerResolver(config)
}
