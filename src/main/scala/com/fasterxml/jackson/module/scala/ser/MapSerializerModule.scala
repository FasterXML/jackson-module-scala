package __foursquare_shaded__.com.fasterxml.jackson.module.scala.ser

import __foursquare_shaded__.com.fasterxml.jackson.databind._
import __foursquare_shaded__.com.fasterxml.jackson.databind.`type`.{MapLikeType, TypeFactory}
import __foursquare_shaded__.com.fasterxml.jackson.databind.jsontype.TypeSerializer
import __foursquare_shaded__.com.fasterxml.jackson.databind.ser.Serializers
import __foursquare_shaded__.com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import __foursquare_shaded__.com.fasterxml.jackson.databind.util.StdConverter
import __foursquare_shaded__.com.fasterxml.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType, config: SerializationConfig)
  extends StdConverter[Map[_,_],java.util.Map[_,_]]
{
  def convert(value: Map[_,_]): java.util.Map[_,_] = {
    val m = if (config.isEnabled(SerializationFeature.WRITE_NULL_MAP_VALUES)) {
      value
    } else {
      value.filter(_._2 != None)
    }
    m.asJava
  }


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
    else new StdDelegatingSerializer(new MapConverter(mapLikeType, config))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  this += MapSerializerResolver
}
