package com.fasterxml.jackson.module.scala.deser

import java.lang.IllegalStateException

import com.fasterxml.jackson.core.{JsonParser, JsonToken};

import com.fasterxml.jackson.databind._;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import com.fasterxml.jackson.module.scala.JacksonModule
import deser.{ContextualDeserializer, Deserializers}

private class TupleDeserializer(javaType: JavaType,
                                config: DeserializationConfig,
                                valueDeserializers: Seq[JsonDeserializer[Object]])
  extends StdDeserializer[Product](classOf[Product]) with ContextualDeserializer {

  val cls = javaType.getRawClass
  val ctors = cls.getConstructors
  if (ctors.length > 1) throw new IllegalStateException("Tuple should have only one constructor")
  val ctor = ctors.head

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    // For now, the dumb and simple route of assuming we don't have the right deserializers.
    // This will probably result in duplicate deserializers, but it's safer than assuming
    // a current non-empty seqeunce of valueDeserializers is correct.
    val paramTypes = for (i <- 0 until javaType.containedTypeCount()) yield javaType.containedType(i)

    val paramDesers = paramTypes map (ctxt.findContextualValueDeserializer(_, property))

    new TupleDeserializer(javaType, config, paramDesers)
  }


  def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {
    // Ok: must point to START_ARRAY (or equivalent)
    if (!jp.isExpectedStartArrayToken) {
      throw ctxt.mappingException(javaType.getRawClass)
    }

    val params = valueDeserializers map { deser =>
      jp.nextToken()
      deser.deserialize(jp, ctxt)
    }

    ctor.newInstance(params: _*).asInstanceOf[Product]
  }

}

private object TupleDeserializerResolver extends Deserializers.Base {

  def PRODUCT = classOf[Product]

  override def findBeanDeserializer(javaType: JavaType,
                                    config: DeserializationConfig,
                                    beanDesc: BeanDescription): JsonDeserializer[_] = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleDeserializer(javaType, config, Seq.empty)
  }
}

/**
 * Adds deserialization support for Scala Tuples.
 */
trait TupleDeserializerModule extends JacksonModule {
  this += TupleDeserializerResolver
}