package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.{ReferenceType, TypeFactory}
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder
import com.fasterxml.jackson.module.scala.ScalaModule
import com.fasterxml.jackson.module.scala.modifiers.OptionTypeModifierModule

private class OptionDeserializer(fullType: JavaType,
                                 valueTypeDeserializer: Option[TypeDeserializer],
                                 valueDeserializer: Option[ValueDeserializer[AnyRef]],
                                 beanProperty: Option[BeanProperty] = None)
  extends StdDeserializer[Option[AnyRef]](fullType) {

  override def getValueType: JavaType = fullType

  override def getNullValue(ctxt: DeserializationContext): Option[AnyRef] = None

  private[this] def withResolved(fullType: JavaType,
                                 typeDeser: Option[TypeDeserializer],
                                 valueDeser: Option[ValueDeserializer[_]],
                                 beanProperty: Option[BeanProperty]): OptionDeserializer = {
    if (fullType == this.fullType &&
      typeDeser == this.valueTypeDeserializer &&
      valueDeser == this.valueDeserializer &&
      beanProperty == this.beanProperty) {
      return this
    }
    new OptionDeserializer(fullType, typeDeser, valueDeser.asInstanceOf[Option[ValueDeserializer[AnyRef]]], beanProperty)
  }

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty): ValueDeserializer[Option[AnyRef]] = {
    val typeDeser = valueTypeDeserializer.map(_.forProperty(property))
    var deser = valueDeserializer
    var typ = fullType

    def refdType() = Option(typ.getContentType).getOrElse(TypeFactory.unknownType())

    if (deser.isEmpty) {
      if (property != null) {
        val intr = ctxt.getAnnotationIntrospector
        val member = property.getMember
        if (intr != null && member != null) {
          typ = intr.refineDeserializationType(ctxt.getConfig, member, typ)
        }
        deser = Option(ctxt.findContextualValueDeserializer(refdType(), property))
      }
    } else { // otherwise directly assigned, probably not contextual yet:
      deser = Option(ctxt.handleSecondaryContextualization(deser.get, property, refdType()).asInstanceOf[ValueDeserializer[AnyRef]])
    }

    withResolved(typ, typeDeser, deser, Option(property))
  }

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Option[AnyRef] = {
    val deser = valueDeserializer.getOrElse(ctxt.findContextualValueDeserializer(fullType.getContentType, beanProperty.orNull))
    val refd: AnyRef = valueTypeDeserializer match {
      case None => deser.deserialize(p, ctxt)
      case Some(vtd) => deser.deserializeWithType(p, ctxt, vtd)
    }
    Option(refd)
  }

  override def deserializeWithType(jp: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer): Option[AnyRef] = {
    val t = jp.currentToken()
    if (t == JsonToken.VALUE_NULL) {
      getNullValue(ctxt)
    } else {
      typeDeserializer.deserializeTypedFromAny(jp, ctxt).asInstanceOf[Option[AnyRef]]
    }
  }
}

private class OptionDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {

  private val OPTION = classOf[Option[AnyRef]]

  override def findReferenceDeserializer(refType: ReferenceType,
                                         config: DeserializationConfig,
                                         beanDesc: BeanDescription,
                                         contentTypeDeserializer: TypeDeserializer,
                                         contentDeserializer: ValueDeserializer[_]): ValueDeserializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) None.orNull
    else {
      val elementType = refType.getContentType
      val typeDeser = Option(contentTypeDeserializer).orElse(Option(elementType.getTypeHandler.asInstanceOf[TypeDeserializer]))
      val valDeser = Option(contentDeserializer).orElse(Option(elementType.getValueHandler)).asInstanceOf[Option[ValueDeserializer[AnyRef]]]
      new OptionDeserializer(refType, typeDeser, valDeser)
    }
  }

  override def hasDeserializerFor(config: DeserializationConfig, valueType: Class[_]): Boolean = {
    OPTION.isAssignableFrom(valueType)
  }
}

trait OptionDeserializerModule extends OptionTypeModifierModule {
  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new OptionDeserializerResolver(config)
      builder.build()
    }
  }
}

object OptionDeserializerModule extends OptionDeserializerModule
