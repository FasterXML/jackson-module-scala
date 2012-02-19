package com.fasterxml.jackson.module.scala.deser

import java.lang.IllegalStateException

import com.fasterxml.jackson.core.{JsonParser, JsonToken};

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.deser.{Deserializers, StdDeserializer};
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import com.fasterxml.jackson.module.scala.JacksonModule

private class TupleDeserializer(javaType: JavaType,
                                config: DeserializationConfig,
                                provider: DeserializerProvider,
                                property: BeanProperty)
  extends StdDeserializer[Product](classOf[Product]) {

  val cls = javaType.getRawClass
  val ctors = cls.getConstructors
  if (ctors.length > 1) throw new IllegalStateException("Tuple should have only one constructor")
  val ctor = ctors.head
  val paramTypes = for (i <- 0 until javaType.containedTypeCount()) yield javaType.containedType(i)

  val paramDesers = paramTypes map { paramType =>
    provider.findValueDeserializer(config, paramType, property)
  }

  def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {
    // Ok: must point to START_ARRAY (or equivalent)
    if (!jp.isExpectedStartArrayToken) {
      throw ctxt.mappingException(javaType.getRawClass)
    }

    val params = paramDesers map { deser =>
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
                                    provider: DeserializerProvider,
                                    beanDesc: BeanDescription,
                                    property: BeanProperty): JsonDeserializer[_] = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleDeserializer(javaType, config, provider, property)
  }
}

/**
 * Adds deserialization support for Scala Tuples.
 */
trait TupleDeserializerModule extends JacksonModule {
  this += TupleDeserializerResolver
}