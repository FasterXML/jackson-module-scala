package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.{MapLikeType, TypeFactory}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType, config: SerializationConfig)
  extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  // Making this an inner class avoids deserializaion errors when polymorphic typing
  // is enabled. In Scala 2.12 `delegate.asJava` happened to be an inner class but
  // this implementation detail changed in 2.13.
  //
  // Tested in DefaultTypingMapDeserializerTest
  private class MapWrapper[A, B](delegate: Map[A, B]) extends java.util.AbstractMap[A, B] {
    private val wrapped = delegate.asJava

    override def entrySet(): java.util.Set[java.util.Map.Entry[A, B]] = wrapped.entrySet()
  }

  def convert(value: Map[_,_]): java.util.Map[_,_] = {
    val m = if (config.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
      value
    } else {
      value.filter(_._2 != None)
    }
    new MapWrapper(m)
  }


  override def getInputType(factory: TypeFactory) = inputType

  override def getOutputType(factory: TypeFactory) =
    factory.constructMapType(classOf[java.util.Map[_,_]], inputType.getKeyType, inputType.getContentType)
      .withTypeHandler(inputType.getTypeHandler)
      .withValueHandler(inputType.getValueHandler)
}

private object MapSerializerResolver extends Serializers.Base {

  private val BASE_CLASS = classOf[collection.Map[_,_]]
  private val JSONSERIALIZABLE_CLASS = classOf[JsonSerializable]

  override def findMapLikeSerializer(config: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {


    val rawClass = mapLikeType.getRawClass

    if (!BASE_CLASS.isAssignableFrom(rawClass) || JSONSERIALIZABLE_CLASS.isAssignableFrom(rawClass)) None.orNull
    else new StdDelegatingSerializer(new MapConverter(mapLikeType, config))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  override def getModuleName: String = "MapSerializerModule"
  this += MapSerializerResolver
}
