package com.fasterxml.jackson.module.scala.ser

import scala.collection.Map
import scala.collection.JavaConverters._

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.{BeanDescription, BeanProperty, JavaType, JsonSerializer, SerializationConfig, SerializerProvider};
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.{ContainerSerializer, ContextualSerializer, ResolvableSerializer, Serializers};
import com.fasterxml.jackson.databind.`type`.MapLikeType;
import com.fasterxml.jackson.databind.ser.std.{MapSerializer => JacksonMapSerializer}

import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule;

private class MapSerializer(mapClass: Class[collection.Map[_,_]],
                            mapLikeType: MapLikeType,
                            vts: TypeSerializer,
                            var keySer: JsonSerializer[AnyRef],
                            valueSer: JsonSerializer[AnyRef])
  extends ContainerSerializer[collection.Map[_,_]](mapClass) with ResolvableSerializer with ContextualSerializer {

  var mapSerializer = JacksonMapSerializer.construct(null, mapLikeType, false, vts, keySer, valueSer);

  def _withValueTypeSerializer(newVts: TypeSerializer): ContainerSerializer[_] =
    new MapSerializer(mapClass, mapLikeType, newVts, keySer, valueSer)

  def serialize(value: Map[_, _], jgen: JsonGenerator, provider: SerializerProvider) {
    mapSerializer.serialize(value.asJava, jgen, provider)
  }

  // MapSerializer can't be internally immutable because of this API
  def resolve(provider: SerializerProvider) {
    if (keySer == null) {
      // !!! 18-Feb-2012, tatu: Can not yet properly resolve KeySerializer here: should do in 'createContextual!'
      keySer = provider.findKeySerializer(mapLikeType.getKeyType, null);
      mapSerializer = JacksonMapSerializer.construct(null, mapLikeType, false, vts, keySer, valueSer);
    }
  }
}

private object MapSerializerResolver extends Serializers.Base {

  val BASE = classOf[collection.Map[_,_]]

  override def findMapLikeSerializer(config: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription,
                                     property: BeanProperty,
                                     keySerializer: JsonSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: JsonSerializer[AnyRef]): JsonSerializer[_] = {


    val rawClass = mapLikeType.getRawClass

    if (BASE.isAssignableFrom(rawClass))
      new MapSerializer(rawClass.asInstanceOf[Class[collection.Map[_,_]]], mapLikeType,
        elementTypeSerializer, keySerializer, elementValueSerializer)
    else null
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}