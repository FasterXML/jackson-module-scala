package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.JacksonModule

private class TupleSerializer extends JsonSerializer[Product] {

  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    jgen.writeStartArray()
    value.productIterator.foreach(provider.defaultSerializeValue(_, jgen))
    jgen.writeEndArray()
  }
}

private object TupleSerializerResolver extends Serializers.Base {

  private val PRODUCT = classOf[Product]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription) = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) None.orNull else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) None.orNull else
    new TupleSerializer
  }

}

trait TupleSerializerModule extends JacksonModule {
  override def getModuleName: String = "TupleSerializerModule"
  this += (_ addSerializers TupleSerializerResolver)
}
