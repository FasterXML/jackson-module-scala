package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.deser.EitherDeserializer.ElementDeserializerConfig


private class EitherDeserializer(javaType: JavaType,
                                 config: DeserializationConfig,
                                 leftDeserializerConfig: ElementDeserializerConfig,
                                 rightDeserializerConfig: ElementDeserializerConfig)
  extends StdDeserializer[Either[AnyRef, AnyRef]](classOf[Either[AnyRef, AnyRef]])
  with ContextualDeserializer {

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {

    def deserializerConfigFor(param: Int, inType: JavaType, property: BeanProperty): ElementDeserializerConfig = {
      val containedType = javaType.containedType(param)

      val paramDeserializer = Option( ctxt.findContextualValueDeserializer(containedType, property) )
      val typeDeserializer = Option(property).map(p => BeanDeserializerFactory.instance.findPropertyTypeDeserializer(ctxt.getConfig, containedType, p.getMember) )

      ElementDeserializerConfig( paramDeserializer, typeDeserializer )
    }

    javaType.containedTypeCount match {
      case 2 =>
        val leftDeserializerConfig = deserializerConfigFor(0, javaType, property)
        val rightDeserializerConfig = deserializerConfigFor(1, javaType, property)
        new EitherDeserializer(javaType, config, leftDeserializerConfig, rightDeserializerConfig)
      case _ => this
    }
  }

  private def deserializeValue(`type`: JsonToken, config: ElementDeserializerConfig, jp: JsonParser, ctxt: DeserializationContext) =
    (config, `type`) match {
      case (_, JsonToken.VALUE_NULL) => null
      case (ElementDeserializerConfig(Some(ed), Some(td)), _) =>
        ed.deserializeWithType(jp, ctxt, td)
      case (ElementDeserializerConfig(Some(ed), _), _) => ed.deserialize(jp, ctxt)
      case (_, _) => throw ctxt.mappingException(javaType.getRawClass)
    }

  private def deserializeEither(jp: JsonParser, ctxt: DeserializationContext) = {
    jp.nextToken()

    val key = jp.getCurrentName
    val `type` = jp.nextToken()

    key match {
      case ("l") => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
      case ("r") => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
      case _ => throw ctxt.mappingException(javaType.getRawClass)
    }
  }

  def deserialize(jp: JsonParser, ctxt: DeserializationContext) = deserializeEither(jp, ctxt)
  override def deserializeWithType(jp: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer)  = deserializeEither(jp, ctxt)
}

private object EitherDeserializer {
  case class ElementDeserializerConfig(deserializer: Option[JsonDeserializer[AnyRef]], typeDeseriazlier: Option[TypeDeserializer])

  object ElementDeserializerConfig {
    val empty = ElementDeserializerConfig(None, None)
  }
}


private object EitherDeserializerResolver extends Deserializers.Base {

  private val EITHER = classOf[Either[_, _]]

  override def findBeanDeserializer(`type`: JavaType, config: DeserializationConfig, beanDesc: BeanDescription) = {
    val rawClass = `type`.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) null
    else new EitherDeserializer( `type`, config, ElementDeserializerConfig.empty, ElementDeserializerConfig.empty )
  }
}

trait EitherDeserializerModule extends JacksonModule {
  this += EitherDeserializerResolver
}
