package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.languageFeature.postfixOps

private class TupleSerializer extends ValueSerializer[Product] {

  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeStartArray()
    value.productIterator.foreach(jgen.writePOJO _)
    jgen.writeEndArray()
  }
}

private object TupleSerializerResolver extends Serializers.Base {

  private val PRODUCT = classOf[Product]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription,
                              formatOverrides: JsonFormat.Value) = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleSerializer
  }

}

trait TupleSerializerModule extends JacksonModule {
  this += (_ addSerializers TupleSerializerResolver)
}
