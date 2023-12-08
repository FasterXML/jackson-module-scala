package tools.jackson.module.scala.ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.ser.Serializers
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.Implicits._
import tools.jackson.module.scala.{JacksonModule, JsonScalaEnumeration, ScalaModule}

trait ContextualEnumerationSerializer {
  self: ValueSerializer[_] =>

  override def createContextual(serializerProvider: SerializerProvider, beanProperty: BeanProperty): ValueSerializer[_] =
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
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider) = {
    val parentEnum = value.asInstanceOf[AnyRef].getClass.getSuperclass.getDeclaredFields.find( f => f.getName == "$outer" )
      .getOrElse(throw new RuntimeException("failed to find $outer field on Enumeration class"))
    val enumClass = parentEnum.get(value).getClass.getName stripSuffix "$"
    jgen.writeStartObject()
    jgen.writeStringProperty("enumClass", enumClass)
    jgen.writeStringProperty("value", value.toString)
    jgen.writeEndObject()
  }
}

private class AnnotatedEnumerationSerializer extends ValueSerializer[scala.Enumeration#Value] with ContextualEnumerationSerializer {
  override def serialize(value: scala.Enumeration#Value, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    provider.writeValue(jgen, value.toString)
  }
}

private class EnumerationSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  override def findSerializer(serializationConfig: SerializationConfig,
                              javaType: JavaType,
                              beanDescription: BeanDescription,
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
