package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.deser.{BeanDeserializerFactory, ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.languageFeature.postfixOps

private class TupleDeserializer(javaType: JavaType,
                                config: DeserializationConfig,
                                valueDeserializers: Seq[JsonDeserializer[Object]] = Nil,
                                typeDeserializers: Seq[TypeDeserializer] = Nil)
  extends StdDeserializer[Product](classOf[Product]) with ContextualDeserializer {

  val cls = javaType.getRawClass
  val ctors = cls.getConstructors
  if (ctors.length != 1) throw new IllegalStateException("Tuple should have exactly one constructor")
  val ctor = ctors.head

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    // For now, the dumb and simple route of assuming we don't have the right deserializers.
    // This will probably result in duplicate deserializers, but it's safer than assuming
    // a current non-empty sequence of valueDeserializers is correct.
    val paramTypes = for (i <- 0 until javaType.containedTypeCount()) yield javaType.containedType(i)

    val paramDesers = paramTypes map (ctxt.findContextualValueDeserializer(_, property))

    val typeDesers: Seq[TypeDeserializer] = {
      val factory = BeanDeserializerFactory.instance
      if (property != null) {
        paramTypes map (factory.findPropertyTypeDeserializer(ctxt.getConfig, _, property.getMember))
      } else {
        paramTypes map (factory.findTypeDeserializer(config, _))
      }
    }

    new TupleDeserializer(javaType, config, paramDesers, typeDesers)
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
        ctxt.wrongTokenException(jp, JsonToken.END_ARRAY,
          "expected closing END_ARRAY after deserialized value")
        // never gets here
        null
      } else {
        ctor.newInstance(params: _*).asInstanceOf[Product]
      }
    } else {
      ctxt.handleUnexpectedToken(javaType.getRawClass, jp).asInstanceOf[Product]
    }
  }
}

private object TupleDeserializerResolver extends Deserializers.Base {

  private val PRODUCT = classOf[Product]

  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription): JsonDeserializer[_] = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) None.orNull else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) None.orNull else
    new TupleDeserializer(javaType, config)
  }
}

/**
 * Adds deserialization support for Scala Tuples.
 */
trait TupleDeserializerModule extends JacksonModule {
  this += TupleDeserializerResolver
}
