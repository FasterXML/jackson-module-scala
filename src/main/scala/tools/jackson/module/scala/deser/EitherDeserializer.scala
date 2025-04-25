package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.ReferenceType
import tools.jackson.databind.deser._
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.jsontype.TypeDeserializer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.deser.EitherDeserializer.ElementDeserializerConfig

private class EitherDeserializer(javaType: JavaType,
                                 deserializationConfig: DeserializationConfig,
                                 leftDeserializerConfig: ElementDeserializerConfig,
                                 rightDeserializerConfig: ElementDeserializerConfig)
  extends StdDeserializer[Either[AnyRef, AnyRef]](classOf[Either[AnyRef, AnyRef]]) {

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): ValueDeserializer[Either[AnyRef, AnyRef]] = {

    def deserializerConfigFor(param: Int, property: BeanProperty): ElementDeserializerConfig = {
      val containedType = javaType.containedType(param)

      val paramDeserializer = Option( ctxt.findContextualValueDeserializer(containedType, property) )
      val typeDeserializer = Option(property).flatMap(p => Option(ctxt.findPropertyTypeDeserializer(containedType, p.getMember)) )

      ElementDeserializerConfig( paramDeserializer.map(_.asInstanceOf[ValueDeserializer[AnyRef]]), typeDeserializer )
    }

    javaType.containedTypeCount match {
      case 2 =>
        val leftDeserializerConfig = deserializerConfigFor(0, property)
        val rightDeserializerConfig = deserializerConfigFor(1, property)
        new EitherDeserializer(javaType, deserializationConfig, leftDeserializerConfig, rightDeserializerConfig)
      case 1 =>
        if (javaType.getBindings.getBoundName(0) == "A") {
          val leftDeserializerConfig = deserializerConfigFor(0, property)
          new EitherDeserializer(javaType, deserializationConfig, leftDeserializerConfig, ElementDeserializerConfig.empty)
        } else {
          val rightDeserializerConfig = deserializerConfigFor(0, property)
          new EitherDeserializer(javaType, deserializationConfig, ElementDeserializerConfig.empty, rightDeserializerConfig)
        }
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
      case (_, _) => ctxt.handleUnexpectedToken(javaType, jp)
    }

  private def deserializeEither(jp: JsonParser, ctxt: DeserializationContext): Either[AnyRef, AnyRef] = {
    jp.currentToken() match {
      case JsonToken.START_OBJECT =>
        val key = jp.nextName()
        val `type` = jp.nextToken()

        val result = key match {
          case "l" => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case "left" => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case "r" => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case "right" => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case _ => ctxt.handleUnexpectedToken(javaType, jp).asInstanceOf[Either[AnyRef, AnyRef]]
        }

        // consume END_OBJECT
        jp.nextToken()

        result
      case JsonToken.START_ARRAY =>
        val key = jp.nextStringValue()
        val `type` = jp.nextToken()

        val result = key match {
          case "l" => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case "left" => Left(deserializeValue(`type`, leftDeserializerConfig, jp, ctxt))
          case "r" => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
          case "right" => Right(deserializeValue(`type`, rightDeserializerConfig, jp, ctxt))
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
  case class ElementDeserializerConfig(deserializer: Option[ValueDeserializer[AnyRef]], typeDeseriazlier: Option[TypeDeserializer])

  object ElementDeserializerConfig {
    val empty = ElementDeserializerConfig(None, None)
  }
}


private class EitherDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {

  private val EITHER = classOf[Either[_, _]]

  override def findBeanDeserializer(`type`: JavaType, deserializationConfig: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[_] = {
    val rawClass = `type`.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) {
      super.findBeanDeserializer(`type`, deserializationConfig, beanDesc)
    } else {
      new EitherDeserializer(`type`, deserializationConfig, ElementDeserializerConfig.empty, ElementDeserializerConfig.empty)
    }
  }

  override def findReferenceDeserializer(refType: ReferenceType, deserializationConfig: DeserializationConfig,
                                         beanDesc: BeanDescription.Supplier, contentTypeDeserializer: TypeDeserializer,
                                         contentDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
    val rawClass = refType.getRawClass

    if (!EITHER.isAssignableFrom(rawClass)) {
      super.findReferenceDeserializer(refType, deserializationConfig, beanDesc, contentTypeDeserializer, contentDeserializer)
    } else {
      new EitherDeserializer(refType, deserializationConfig, ElementDeserializerConfig.empty, ElementDeserializerConfig.empty)
    }
  }

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    EITHER.isAssignableFrom(valueType)
  }
}

trait EitherDeserializerModule extends JacksonModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EitherDeserializerResolver(config)
    builder.build()
  }
}

object EitherDeserializerModule extends EitherDeserializerModule
