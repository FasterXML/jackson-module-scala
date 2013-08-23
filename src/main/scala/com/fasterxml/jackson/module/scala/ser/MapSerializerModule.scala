package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind.`type`.{TypeFactory, MapType, MapLikeType}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.databind.{JavaType, BeanDescription, SerializationConfig, JsonSerializer}
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType)
  extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  def convert(value: Map[_,_]) = value.asJava

  override def getInputType(factory: TypeFactory) = inputType

  override def getOutputType(factory: TypeFactory) =
    factory.constructMapType(classOf[java.util.Map[_,_]], inputType.getKeyType, inputType.getContentType)
      .withTypeHandler(inputType.getTypeHandler)
      .withValueHandler(inputType.getValueHandler)
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
    else new StdDelegatingSerializer(new MapConverter(mapLikeType))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}