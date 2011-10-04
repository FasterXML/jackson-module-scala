package com.fasterxml.jackson.module.scala.ser

import org.codehaus.jackson.map.`type`.MapLikeType
import org.codehaus.jackson.map.ser.std.{ContainerSerializerBase, MapSerializer => JacksonMapSerializer}
import scala.collection.Map
import org.codehaus.jackson.JsonGenerator
import scala.collection.JavaConverters._
import com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule
import org.codehaus.jackson.map.{ResolvableSerializer, SerializerProvider, TypeSerializer, JsonSerializer, BeanProperty, BeanDescription, SerializationConfig, Serializers}

private class MapSerializer(mapClass: Class[collection.Map[_,_]],
                            mapLikeType: MapLikeType,
                            vts: TypeSerializer,
                            property: BeanProperty,
                            var keySer: JsonSerializer[AnyRef],
                            valueSer: JsonSerializer[AnyRef])
  extends ContainerSerializerBase[collection.Map[_,_]](mapClass) with ResolvableSerializer {

  var mapSerializer = JacksonMapSerializer.construct(null, mapLikeType, false, vts, property, keySer, valueSer);

  def _withValueTypeSerializer(newVts: TypeSerializer): ContainerSerializerBase[_] =
    new MapSerializer(mapClass, mapLikeType, newVts, property, keySer, valueSer)

  def serialize(value: Map[_, _], jgen: JsonGenerator, provider: SerializerProvider) {
    mapSerializer.serialize(value.asJava, jgen, provider)
  }

  // MapSerializer can't be internally immutable because of this API
  def resolve(provider: SerializerProvider) {
    if (keySer == null) {
      keySer = provider.findKeySerializer(mapLikeType.getKeyType, property)
      mapSerializer = JacksonMapSerializer.construct(null, mapLikeType, false, vts, property, keySer, valueSer);
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
        elementTypeSerializer, property, keySerializer, elementValueSerializer)
    else null
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}