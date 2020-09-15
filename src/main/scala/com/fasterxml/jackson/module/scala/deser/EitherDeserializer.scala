package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.ReferenceType
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

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer[Either[AnyRef, AnyRef]] = {

    def deserializerConfigFor(param: Int, inType: JavaType, property: BeanProperty): ElementDeserializerConfig = {
      val containedType = javaType.containedType(param)

      val paramDeserializer = Option( ctxt.findContextualValueDeserializer(containedType, property) )
      val typeDeserializer = Option(property).flatMap(p => Option(BeanDeserializerFactory.instance.findPropertyTypeDeserializer(ctxt.getConfig, containedType, p.getMember)) )

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
      case (ElementDeserializerConfig(Some(ed), _), JsonToken.VALUE_NULL) if ed.isInstanceOf[OptionDeserializer] =>
        None
      case (_, JsonToken.VALUE_NULL) => None.orNull
      case (ElementDeserializerConfig(Some(ed), Some(td)), _) =>
        ed.deserializeWithType(jp, ctxt, td)
      case (ElementDeserializerConfig(Some(ed), _), _) => ed.deserialize(jp, ctxt)
      case (_, _) => ctxt.handleUnexpectedToken(javaType.getRawClass, jp)
    }

  private def deserializeEither(jp: JsonParser, ctxt: DeserializationContext): Either[AnyRef, AnyRef] = {
    jp.currentToken() match {
      case JsonToken.START_OBJECT =>
        val key = jp.nextFieldName()
        val `type` = jp.nextToken()

        val result = key match {
          case ("l") => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case ("left") => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case ("r") => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case ("right") => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case _ => ctxt.handleUnexpectedToken(javaType, jp).asInstanceOf[Either[AnyRef, AnyRef]]
        }

        // consume END_OBJECT
        jp.nextToken()

        result
      case JsonToken.START_ARRAY =>
        val key = jp.nextTextValue()
        val `type` = jp.nextToken()

        val result = key match {
          case ("l") => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case ("left") => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case ("r") => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case ("right") => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case _ => ctxt.handleUnexpectedToken(javaType, jp).asInstanceOf[Either[AnyRef, AnyRef]]
        }

        // consume END_ARRAY
        jp.nextToken()

        result
      case _ => ctxt.handleUnexpectedToken(javaType, jp).asInstanceOf[Either[AnyRef, AnyRef]]
    }
  }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Either[AnyRef, AnyRef] =
    deserializeEither(jp, ctxt)
  override def deserializeWithType(jp: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer): Either[AnyRef, AnyRef] =
    deserializeEither(jp, ctxt)
}

private object EitherDeserializer {
  case class ElementDeserializerConfig(deserializer: Option[JsonDeserializer[AnyRef]], typeDeserializer: Option[TypeDeserializer])

  object ElementDeserializerConfig {
    val empty = ElementDeserializerConfig(None, None)
  }
}


private object EitherDeserializerResolver extends Deserializers.Base {

  private val EITHER = classOf[Either[_, _]]

  override def findBeanDeserializer(`type`: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    val rawClass = `type`.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) {
      super.findBeanDeserializer(`type`, config, beanDesc)
    } else {
      new EitherDeserializer( `type`, config, ElementDeserializerConfig.empty, ElementDeserializerConfig.empty )
    }
  }

  override def findReferenceDeserializer(refType: ReferenceType, config: DeserializationConfig,
                                         beanDesc: BeanDescription, contentTypeDeserializer: TypeDeserializer,
                                         contentDeserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    val rawClass = refType.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) {
      super.findReferenceDeserializer(refType, config, beanDesc, contentTypeDeserializer, contentDeserializer)
    } else {
      new EitherDeserializer( refType, config, ElementDeserializerConfig.empty, ElementDeserializerConfig.empty )
    }
  }
}

trait EitherDeserializerModule extends JacksonModule {
  this += EitherDeserializerResolver
}
