package com.fasterxml.jackson.module.scala.ser

import java.lang.reflect.Type

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind._;
import com.fasterxml.jackson.databind.jsontype.{TypeSerializer};
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.ser.{ContainerSerializer, Serializers};

import com.fasterxml.jackson.module.scala.JacksonModule;

private class TupleSerializer(javaType: JavaType, property: BeanProperty)
  extends ContainerSerializer[Product](classOf[Product]) {

  def serialize(value: Product, jgen: JsonGenerator, provider: SerializerProvider)
  {
    jgen.writeStartArray()
    value.productIterator.foreach { item =>
      val itemRef = item.asInstanceOf[AnyRef]
      val itemType = itemRef.getClass
      val ser = provider.findValueSerializer(itemType, property)
      ser.serialize(itemRef, jgen, provider)
    }
    jgen.writeEndArray()
  }

  override def getSchema(provider: SerializerProvider, typeHint: Type) = {
    val o = createSchemaNode("array")
    val a = o.putArray("items")

    (for (i <- 0 to javaType.containedTypeCount()) yield {
        val eltType = javaType.containedType(i)
        provider.findValueSerializer(eltType, property) match {
          case ser: SchemaAware => ser.getSchema(provider, eltType.getRawClass)
          case _ => { createSchemaNode("any") }
        }
    }).foreach(a.add(_))

    o
  }

  def _withValueTypeSerializer(newVts: TypeSerializer) = this
}

private object TupleSerializerResolver extends Serializers.Base {

  private val PRODUCT = classOf[Product]

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription, beanProperty: BeanProperty) = {
    val cls = javaType.getRawClass
    if (!PRODUCT.isAssignableFrom(cls)) null else
    // If it's not *actually* a tuple, it's either a case class or a custom Product
    // which either way we shouldn't handle here.
    if (!cls.getName.startsWith("scala.Tuple")) null else
    new TupleSerializer(javaType, beanProperty)
  }

}

trait TupleSerializerModule extends JacksonModule {
  this += TupleSerializerResolver
}