package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.ser.{Serializers, ValueSerializerModifier}
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.util.NameTransformer
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.deser.EnumDeserializerShared
import tools.jackson.module.scala.util.Scala3EnumInfo

import scala.reflect.Enum

private object EnumSerializer extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit =
    jgen.writeString(value.toString)

}

private object EnumKeySerializer extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit =
    jgen.writeName(value.toString)
}

/**
 * Serializer bound to the declared type of a Scala 3 enum that has parameterized cases. Simple
 * cases are written as their name; parameterized cases are delegated to the serializer of the
 * generated case class, which [[Scala3EnumCaseSerializer]] has tagged with a `type` property.
 */
private case class Scala3EnumSumSerializer(info: Scala3EnumInfo.Info) extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    if (info.parameterizedCaseFor(value.getClass).isDefined) {
      serializationContext.findValueSerializer(value.getClass).serialize(value, jgen, serializationContext)
    } else {
      jgen.writeString(value.toString)
    }
  }
}

/**
 * Wraps the bean serializer of a parameterized Scala 3 enum case so that the case name is written
 * as a `type` property, making the value round-trippable.
 */
private class Scala3EnumCaseSerializer(typeName: String, delegate: ValueSerializer[AnyRef])
  extends ValueSerializer[AnyRef] {

  private lazy val unwrapped = delegate.unwrappingSerializer(NameTransformer.NOP)

  override def resolve(serializationContext: SerializationContext): Unit = delegate.resolve(serializationContext)

  override def createContextual(serializationContext: SerializationContext, property: BeanProperty): ValueSerializer[_] = {
    val contextual = delegate.createContextual(serializationContext, property)
    if (contextual eq delegate) this
    else new Scala3EnumCaseSerializer(typeName, contextual.asInstanceOf[ValueSerializer[AnyRef]])
  }

  override def serialize(value: AnyRef, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    jgen.writeStartObject(value)
    jgen.writeStringProperty(Scala3EnumInfo.TypePropertyName, typeName)
    unwrapped.serialize(value, jgen, serializationContext)
    jgen.writeEndObject()
  }
}

private class EnumSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Enum] = {
    val rawClass = javaType.getRawClass
    if (!EnumDeserializerShared.EnumClass.isAssignableFrom(rawClass)) None.orNull
    else EnumDeserializerShared.taggedSumInfo(rawClass) match {
      // the generated case classes are serialized as beans, tagged by EnumSerializerModifier
      case Some(info) => if (info.parameterizedCaseFor(rawClass).isDefined) None.orNull else Scala3EnumSumSerializer(info)
      case None => if (EnumDeserializerShared.canFindByOrdinal(rawClass)) EnumSerializer else None.orNull
    }
  }
}

private class EnumSerializerModifier(config: ScalaModule.Config) extends ValueSerializerModifier {
  override def modifySerializer(config: SerializationConfig, beanDesc: BeanDescription.Supplier,
                                serializer: ValueSerializer[_]): ValueSerializer[_] = {
    val rawClass = beanDesc.getBeanClass
    EnumDeserializerShared.taggedSumInfo(rawClass).flatMap(_.parameterizedCaseFor(rawClass)) match {
      case Some(enumCase) => new Scala3EnumCaseSerializer(enumCase.name, serializer.asInstanceOf[ValueSerializer[AnyRef]])
      case None => serializer
    }
  }
}

private class EnumKeySerializerResolver(config: ScalaModule.Config) extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Enum] =
    if (EnumDeserializerShared.EnumClass.isAssignableFrom(javaType.getRawClass))
      EnumKeySerializer
    else None.orNull
}

trait EnumSerializerModule extends JacksonModule {
  override def getModuleName: String = "EnumSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumSerializerResolver(config)
    builder += new EnumSerializerModifier(config)
    builder.addKeySerializers(new EnumKeySerializerResolver(config))
    builder.build()
  }
}

object EnumSerializerModule extends EnumSerializerModule
