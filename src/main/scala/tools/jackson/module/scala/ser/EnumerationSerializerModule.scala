package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.{JsonGenerator, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.std.StdScalarSerializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.Implicits._
import tools.jackson.module.scala.{JacksonModule, JsonScalaEnumeration, ScalaModule}

trait ContextualEnumerationSerializer {
  self: ValueSerializer[_] =>

  override def createContextual(SerializationContext: SerializationContext, beanProperty: BeanProperty): ValueSerializer[_] =
    Option(beanProperty)
      .optMap(_.getAnnotation(classOf[JsonScalaEnumeration]))
      .map(_ => new AnnotatedEnumerationSerializer)
      .getOrElse(this)
}

/**
 * The implementation is taken from the code written by Greg Zoller, found here:
 * http://jira.codehaus.org/browse/JACKSON-211
 */
private class EnumerationSerializer extends ValueSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    val enumClass = enumClassName(value)
    jgen.writeStartObject(value)
    serializeContents(value, enumClass, jgen)
    jgen.writeEndObject()
  }

  override def serializeWithType(value: scala.Enumeration#Value, jgen: JsonGenerator, serializationContext: SerializationContext,
                                 typeSer: TypeSerializer): Unit = {
    val enumClass = enumClassName(value)
    val typeIdDef = typeSer.writeTypePrefix(jgen, serializationContext, typeSer.typeId(value, JsonToken.START_OBJECT))
    serializeContents(value, enumClass, jgen)
    typeSer.writeTypeSuffix(jgen, serializationContext, typeIdDef)
  }

  private def enumClassName(value: scala.Enumeration#Value): String = {
    val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" )
      .getOrElse(throw new RuntimeException("failed to find $outer field on Enumeration class"))
    if (!parentEnum.canAccess(value)) {
      // setAccessible is needed for Scala 3.8+ (https://github.com/FasterXML/jackson-module-scala/issues/795)
      parentEnum.setAccessible(true)
    }
    parentEnum.get(value).getClass.getName stripSuffix "$"
  }

  private def serializeContents(value: scala.Enumeration#Value, enumClass: String, jgen: JsonGenerator): Unit = {
    jgen.writeStringProperty("enumClass", enumClass)
    jgen.writeStringProperty("value", value.toString)
  }
}

private class AnnotatedEnumerationSerializer extends StdScalarSerializer[scala.Enumeration#Value](classOf[scala.Enumeration#Value])
  with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, serializationContext: SerializationContext): Unit = {
    serializationContext.writeValue(jgen, value.toString)
  }
}

private class EnumerationSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  override def findSerializer(serializationConfig: SerializationConfig,
                              javaType: JavaType,
                              beanDescription: BeanDescription.Supplier,
                              formatOverrides: JsonFormat.Value): ValueSerializer[_] = {
    val clazz = javaType.getRawClass

    if (classOf[scala.Enumeration#Value].isAssignableFrom(clazz)) {
      new EnumerationSerializer
    } else {
      null
    }
  }

}

trait EnumerationSerializerModule extends JacksonModule {
  override def getModuleName: String = "EnumerationSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumerationSerializerResolver(config)
    builder.build()
  }
}

object EnumerationSerializerModule extends EnumerationSerializerModule
