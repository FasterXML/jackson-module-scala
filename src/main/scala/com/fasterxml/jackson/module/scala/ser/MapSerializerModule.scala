package com.fasterxml.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.{MapLikeType, TypeFactory}
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.scala.ScalaModule
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType, config: SerializationConfig)
  extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  def convert(value: Map[_,_]): java.util.Map[_,_] = value.asJava

  override def getInputType(factory: TypeFactory) = inputType

  override def getOutputType(factory: TypeFactory) =
    factory.constructMapType(classOf[java.util.Map[_,_]], inputType.getKeyType, inputType.getContentType)
      .withTypeHandler(inputType.getTypeHandler)
      .withValueHandler(inputType.getValueHandler)
}

private class MapSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  private val BASE = classOf[collection.Map[_,_]]

  override def findMapLikeSerializer(serializationConfig: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     formatOverrides: JsonFormat.Value,
                                     keySerializer: ValueSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: ValueSerializer[AnyRef]): ValueSerializer[_] = {

    val rawClass = mapLikeType.getRawClass

    if (!BASE.isAssignableFrom(rawClass)) null
    else new StdDelegatingSerializer(new MapConverter(mapLikeType, serializationConfig))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  override protected def initScalaModule(config: ScalaModule.Config): Unit = {
    this += new MapSerializerResolver(config)
  }
}
