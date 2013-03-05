package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.`type`.MapLikeType
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.databind.{BeanDescription, SerializationConfig, JsonSerializer}
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import scala.collection.JavaConverters._
import scala.collection.Map

private object MapConverter extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  def convert(value: Map[_,_]) = value.asJava
}

private object MapSerializerResolver extends Serializers.Base {

  val BASE = classOf[collection.Map[_,_]]

  override def findMapLikeSerializer(config: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {


    val rawClass = mapLikeType.getRawClass

    if (!BASE.isAssignableFrom(rawClass)) null
    else new StdDelegatingSerializer(MapConverter)
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}