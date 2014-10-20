package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.databind.deser._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.JacksonModule


private class EitherDeserializer(javaType: JavaType,
                                 config: DeserializationConfig,
                                 leftDeserializer: Option[JsonDeserializer[Object]] = None,
                                 rightDeserializer: Option[JsonDeserializer[Object]] = None)
  extends StdDeserializer[Either[_, _]](classOf[Either[_, _]])
  with ContextualDeserializer {

  def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    def deserializerFor(loc: Int) =
      Option(ctxt.findContextualValueDeserializer(javaType.containedType(loc), property))

    javaType.containedTypeCount match {
      case 2 => val leftDeser = deserializerFor(0)
        val rightDeser = deserializerFor(1)
        new EitherDeserializer(javaType, config, leftDeser, rightDeser)
      case _ => this
    }
  }

  def deserialize(jp: JsonParser, ctxt: DeserializationContext) = {
    jp.nextToken()

    val key = jp.getCurrentName

    val `type` = jp.nextToken()

    // this is shit, there is probably a better way to handle null values
    // need to understand what the type is without jackson types
    (key, leftDeserializer, rightDeserializer, `type`) match {
      case ("l", _, _, JsonToken.VALUE_NULL) => Left(null)
      case ("r", _, _, JsonToken.VALUE_NULL) => Right(null)
      case ("l", Some(d), _, _) => Left(d.deserialize(jp, ctxt))
      case ("r", _, Some(r), _) => Right(r.deserialize(jp, ctxt))
      case _ => throw ctxt.mappingException(javaType.getRawClass)
    }
  }
}

private object EitherDeserializerResolver extends Deserializers.Base {

  private val EITHER = classOf[Either[_, _]]

  override def findBeanDeserializer(`type`: JavaType, config: DeserializationConfig, beanDesc: BeanDescription) = {
    val rawClass = `type`.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) null
    else new EitherDeserializer(`type`, config)
  }

  override def findMapLikeDeserializer(`type`: MapLikeType, config: DeserializationConfig, beanDesc: BeanDescription, keyDeserializer: KeyDeserializer, elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]) = super.findMapLikeDeserializer(`type`, config, beanDesc, keyDeserializer, elementTypeDeserializer, elementDeserializer)
}

trait EitherDeserializerModule extends JacksonModule {
  this += EitherDeserializerResolver
}
