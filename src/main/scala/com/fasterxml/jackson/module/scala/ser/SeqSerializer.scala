package com.fasterxml.jackson.module.scala.ser

import collection.Seq
import collection.JavaConverters._
import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.ser.ContainerSerializers.{CollectionSerializer, AsArraySerializer}
import org.codehaus.jackson.map.{BeanProperty, JsonSerializer, SerializerProvider, TypeSerializer}
import org.codehaus.jackson.`type`.JavaType

class SeqSerializer(elemType: JavaType, staticTyping: Boolean, vts: Option[TypeSerializer], property: BeanProperty, valueSerializer: Option[JsonSerializer[AnyRef]])
  extends AsArraySerializer[collection.Seq[Any]](classOf[SeqSerializer],elemType,staticTyping, vts.orNull, property, valueSerializer.orNull) {

  val collectionSerializer =
    new CollectionSerializer(elemType, staticTyping, vts.orNull, property, valueSerializer.orNull)

  def serializeContents(value: Seq[Any], jgen: JsonGenerator, provider: SerializerProvider)
  {
    collectionSerializer.serializeContents(value.asJavaCollection, jgen, provider)
  }

  override def _withValueTypeSerializer(newVts: TypeSerializer) =
    new SeqSerializer(elemType, staticTyping, Option(newVts), property, valueSerializer)
}