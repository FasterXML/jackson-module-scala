package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.{MapLikeType, TypeFactory}
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.std.StdDelegatingSerializer
import tools.jackson.databind.util.StdConverter
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.ScalaModule
import tools.jackson.module.scala.modifiers.MapTypeModifierModule

import scala.collection.JavaConverters._
import scala.collection.Map

private class MapConverter(inputType: JavaType, serializationConfig: SerializationConfig)
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

  private val BASE_CLASS = classOf[collection.Map[_,_]]
  private val JACKSONSERIALIZABLE_CLASS = classOf[JacksonSerializable]

  override def findMapLikeSerializer(serializationConfig: SerializationConfig,
                                     mapLikeType : MapLikeType,
                                     beanDesc: BeanDescription.Supplier,
                                     formatOverrides: JsonFormat.Value,
                                     keySerializer: ValueSerializer[AnyRef],
                                     elementTypeSerializer: TypeSerializer,
                                     elementValueSerializer: ValueSerializer[AnyRef]): ValueSerializer[_] = {

    val rawClass = mapLikeType.getRawClass

    if (!BASE_CLASS.isAssignableFrom(rawClass) || JACKSONSERIALIZABLE_CLASS.isAssignableFrom(rawClass)) None.orNull
    else new StdDelegatingSerializer(new MapConverter(mapLikeType, serializationConfig))
  }

}

trait MapSerializerModule extends MapTypeModifierModule {
  override def getModuleName: String = "MapSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new MapSerializerResolver(config)
      builder.build()
    }
  }
}

object MapSerializerModule extends MapSerializerModule
