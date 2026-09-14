package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.{Serializers, ValueSerializerModifier}
import tools.jackson.databind.ser.std.StdScalarSerializer
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule, Scala3EnumSupportState}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.deser.EnumDeserializerShared
import tools.jackson.module.scala.util.Scala3EnumInfo

import scala.reflect.Enum

private[scala] object EnumSerializerShared {
  /**
   * The class a type id for `value` names. The simple cases of an enum are all instances of one
   * anonymous subclass of it, which nothing can be declared as, so the enum itself is named - as
   * databind names the enum for a Java constant with a body of its own.
   */
  def enumClassOf(value: AnyRef): Class[_] = {
    var clazz: Class[_] = value.getClass
    while (clazz.isAnonymousClass && clazz.getSuperclass != null) clazz = clazz.getSuperclass
    clazz
  }
}

private class EnumSerializer extends StdScalarSerializer[Enum](EnumDeserializerShared.EnumClass) {
  private val rootTypeSerializers = new RootTypeSerializers(EnumSerializerShared.enumClassOf)

  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    val typeSer = rootTypeSerializers.rootTypeSerializer(jgen, serializationContext, value.asInstanceOf[AnyRef])
    if (typeSer == null) jgen.writeString(value.toString)
    else serializeWithType(value, jgen, serializationContext, typeSer)
  }

  override def serializeWithType(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    val typeIdDef = typeSer.writeTypePrefix(jgen, serializationContext,
      typeSer.typeId(value, EnumSerializerShared.enumClassOf(value.asInstanceOf[AnyRef]), JsonToken.VALUE_STRING))
    jgen.writeString(value.toString)
    typeSer.writeTypeSuffix(jgen, serializationContext, typeIdDef)
  }
}

private object EnumKeySerializer extends ValueSerializer[Enum] {
  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit =
    jgen.writeName(value.toString)
}

/**
 * Serializer bound to the declared type of a Scala 3 enum that has parameterized cases. Simple
 * cases are written as their name; parameterized cases are delegated to the serializer of the
 * generated case class, which [[TypeTaggedSerializer]] has tagged with an `@type` property.
 */
private case class Scala3EnumSumSerializer(info: Scala3EnumInfo.Info) extends ValueSerializer[Enum] {
  private val rootTypeSerializers = new RootTypeSerializers(_ => info.rootClass)

  override def serialize(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    val typeSer = rootTypeSerializers.rootTypeSerializer(jgen, serializationContext, value.asInstanceOf[AnyRef])
    if (typeSer != null) {
      serializeWithType(value, jgen, serializationContext, typeSer)
    } else if (info.parameterizedCaseFor(value.getClass).isDefined) {
      serializationContext.findValueSerializer(value.getClass).serialize(value, jgen, serializationContext)
    } else {
      jgen.writeString(value.toString)
    }
  }

  // a parameterized case is typed by its own class; a simple case is a scalar typed as the enum
  override def serializeWithType(value: Enum, jgen: JsonGenerator, serializationContext: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    if (info.parameterizedCaseFor(value.getClass).isDefined) {
      serializationContext.findValueSerializer(value.getClass).serializeWithType(value, jgen, serializationContext, typeSer)
    } else {
      val typeIdDef = typeSer.writeTypePrefix(jgen, serializationContext,
        typeSer.typeId(value, info.rootClass, JsonToken.VALUE_STRING))
      jgen.writeString(value.toString)
      typeSer.writeTypeSuffix(jgen, serializationContext, typeIdDef)
    }
  }
}

private class EnumSerializerResolver(config: ScalaModule.Config, enumInfo: Scala3EnumInfo) extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[Enum] = {
    val rawClass = javaType.getRawClass
    if (!EnumDeserializerShared.EnumClass.isAssignableFrom(rawClass)) None.orNull
    else enumInfo.taggedSumInfo(rawClass) match {
      // the generated case classes are serialized as beans, tagged by EnumSerializerModifier
      case Some(info) => if (info.parameterizedCaseFor(rawClass).isDefined) None.orNull else Scala3EnumSumSerializer(info)
      case None => if (EnumDeserializerShared.canFindByOrdinal(rawClass)) new EnumSerializer else None.orNull
    }
  }
}

private class EnumSerializerModifier(config: ScalaModule.Config, enumInfo: Scala3EnumInfo) extends ValueSerializerModifier {
  override def modifySerializer(config: SerializationConfig, beanDesc: BeanDescription.Supplier,
                                serializer: ValueSerializer[_]): ValueSerializer[_] = {
    val rawClass = beanDesc.getBeanClass
    enumInfo.taggedSumInfo(rawClass).flatMap(_.parameterizedCaseFor(rawClass)) match {
      // registering the module twice would otherwise wrap a wrapper, and the inner one would write
      // a whole object where the outer expected only properties
      case Some(_) if serializer.isInstanceOf[TypeTaggedSerializer] => serializer
      case Some(enumCase) =>
        new TypeTaggedSerializer(Scala3EnumInfo.TypePropertyName, enumCase.name, enumInfo.taggedSumInfo(rawClass).get.rootClass,
          serializer.asInstanceOf[ValueSerializer[AnyRef]])
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

trait EnumSerializerModule extends JacksonModule with Scala3EnumSupportState {
  override def getModuleName: String = "EnumSerializerModule"

  protected def serializerInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumSerializerResolver(config, scala3EnumInfo)
    builder += new EnumSerializerModifier(config, scala3EnumInfo)
    builder.addKeySerializers(new EnumKeySerializerResolver(config))
    builder.build()
  }

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] =
    serializerInitializers(config)
}

object EnumSerializerModule extends EnumSerializerModule
