package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.{MapLikeType, TypeFactory}
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.jdk.{MapSerializer => JdkMapSerializer}
import tools.jackson.databind.ser.std.StdConvertingSerializer
import tools.jackson.databind.util.{Converter, StdConverter}
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

/**
 * Serializes a Scala Map by wrapping it as a java.util.Map and delegating to jackson-databind's
 * MapSerializer. The one thing the delegate must not see is the wrapper's class: with polymorphic
 * (e.g. default) typing enabled the type id has to name the Scala Map, or it can never be read back
 * (#643). So the type id is derived from the original value here and only the entries are delegated.
 */
private class ScalaMapSerializer(converter: Converter[AnyRef, _],
                                 delegateType: JavaType,
                                 delegateSerializer: ValueSerializer[_],
                                 property: BeanProperty)
  extends StdConvertingSerializer(converter, delegateType, delegateSerializer, property)
{
  override protected def withDelegate(converter: Converter[AnyRef, _],
                                      delegateType: JavaType,
                                      delegateSerializer: ValueSerializer[_],
                                      property: BeanProperty): StdConvertingSerializer =
    new ScalaMapSerializer(converter, delegateType, delegateSerializer, property)

  override def serializeWithType(value: AnyRef,
                                 gen: JsonGenerator,
                                 ctxt: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    (_delegateSerializer: ValueSerializer[_]) match {
      case mapSerializer: JdkMapSerializer =>
        val delegateValue = convertValue(ctxt, value).asInstanceOf[java.util.Map[_, _]]
        gen.assignCurrentValue(value)
        val typeIdDef = typeSer.writeTypePrefix(gen, ctxt, typeSer.typeId(value, JsonToken.START_OBJECT))
        mapSerializer.serializeWithoutTypeInfo(delegateValue, gen, ctxt)
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef)
      case _ =>
        super.serializeWithType(value, gen, ctxt, typeSer)
    }
  }
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
    else {
      val converter = new MapConverter(mapLikeType, serializationConfig).asInstanceOf[Converter[AnyRef, _]]
      new ScalaMapSerializer(converter, converter.getOutputType(serializationConfig.getTypeFactory), None.orNull, None.orNull)
    }
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
