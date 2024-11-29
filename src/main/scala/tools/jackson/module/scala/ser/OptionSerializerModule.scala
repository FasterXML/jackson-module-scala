package tools.jackson
package module.scala
package ser

import com.fasterxml.jackson.annotation.JsonFormat
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.`type`.ReferenceType
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.jsontype.TypeSerializer
import tools.jackson.databind.ser.Serializers
import tools.jackson.databind.ser.std.ReferenceTypeSerializer
import tools.jackson.databind.util.NameTransformer
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.modifiers.OptionTypeModifierModule

import scala.languageFeature.postfixOps

import scala.util.control.Breaks.{break, breakable}

// This is still here because it is used in other places like EitherSerializer, it is no
// longer used for the Option serializer
object OptionSerializer {
  def useStatic(serializationContext: SerializationContext, property: Option[BeanProperty], referredType: Option[JavaType]): Boolean = {
    if (referredType.isEmpty) false
    // First: no serializer for `Object.class`, must be dynamic
    else if (referredType.get.isJavaLangObject) false
    // but if type is final, might as well fetch
    else if (referredType.get.isFinal) true
    // also: if indicated by typing, should be considered static
    else if (referredType.get.useStaticType()) true
    // if neither, maybe explicit annotation?
    else {
      var result: Option[Boolean] = None
      breakable {
        for (
          ann <- property.flatMap(p => Option(p.getMember));
          intr <- Option(serializationContext.getAnnotationIntrospector)
        ) {
          val typing = intr.findSerializationTyping(serializationContext.getConfig, ann)
          if (typing == JsonSerialize.Typing.STATIC) {
            result = Some(true)
            break()
          }
          if (typing == JsonSerialize.Typing.DYNAMIC) {
            result = Some(false)
            break()
          }
        }
      }
      result match {
        case Some(bool) => bool
        case _ =>
          // and finally, may be forced by global static typing (unlikely...)
          serializationContext.isEnabled(MapperFeature.USE_STATIC_TYPING)
      }
    }
  }

  def findSerializer(serializationContext: SerializationContext, typ: Class[_], prop: Option[BeanProperty]): ValueSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    serializationContext.findTypedValueSerializer(typ, true).asInstanceOf[ValueSerializer[AnyRef]]
  }

  def findSerializer(serializationContext: SerializationContext, typ: JavaType, prop: Option[BeanProperty]): ValueSerializer[AnyRef] = {
    // Important: ask for TYPED serializer, in case polymorphic handling is needed!
    serializationContext.findTypedValueSerializer(typ, true).asInstanceOf[ValueSerializer[AnyRef]]
  }

  def hasContentTypeAnnotation(serializationContext: SerializationContext, property: BeanProperty): Boolean = {
    val intr = serializationContext.getAnnotationIntrospector
    if (property == null || intr == null) {
      false
    } else {
      intr.refineSerializationType(serializationContext.getConfig, property.getMember, property.getType) != null
    }
  }
}

class OptionSerializer(
  refType: ReferenceType,
  staticTyping: Boolean,
  contentTypeSerializer: TypeSerializer,
  contentValueSerializer: ValueSerializer[AnyRef]
) extends ReferenceTypeSerializer[Option[_]](
      refType,
      staticTyping,
      contentTypeSerializer,
      contentValueSerializer
    ) {

  override def withResolved(
    prop: BeanProperty,
    vts: TypeSerializer,
    valueSer: ValueSerializer[_],
    unwrapper: NameTransformer
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      prop,
      vts,
      valueSer,
      unwrapper,
      _suppressableValue,
      _suppressNulls
    )
  }

  override def withContentInclusion(
    suppressableValue: AnyRef,
    suppressNulls: Boolean
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      _property,
      _valueTypeSerializer,
      _valueSerializer,
      _unwrapper,
      suppressableValue,
      suppressNulls
    )
  }

  override protected def _isValuePresent(value: Option[_]): Boolean = value.isDefined

  override protected def _getReferenced(value: Option[_]): AnyRef = value.get.asInstanceOf[AnyRef]

  override protected def _getReferencedIfPresent(value: Option[_]): AnyRef = {
    value.asInstanceOf[Option[AnyRef]].orNull
  }
}

class ResolvedOptionSerializer(
  base: ReferenceTypeSerializer[_],
  property: BeanProperty,
  vts: TypeSerializer,
  valueSer: ValueSerializer[_],
  unwrapper: NameTransformer,
  suppressableValue: AnyRef,
  suppressNulls: Boolean
) extends ReferenceTypeSerializer[Option[_]](
      base,
      property,
      vts,
      valueSer,
      unwrapper,
      suppressableValue,
      suppressNulls
    ) {

  override def withResolved(
    prop: BeanProperty,
    vts: TypeSerializer,
    valueSer: ValueSerializer[_],
    unwrapper: NameTransformer
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      prop,
      vts,
      valueSer,
      unwrapper,
      _suppressableValue,
      _suppressNulls
    )
  }

  override def withContentInclusion(
    suppressableValue: AnyRef,
    suppressNulls: Boolean
  ): ReferenceTypeSerializer[Option[_]] = {

    new ResolvedOptionSerializer(
      this,
      _property,
      _valueTypeSerializer,
      _valueSerializer,
      _unwrapper,
      suppressableValue,
      suppressNulls
    )
  }

  override protected def _isValuePresent(value: Option[_]): Boolean = value.isDefined

  override protected def _getReferenced(value: Option[_]): AnyRef = value.get.asInstanceOf[AnyRef]

  override protected def _getReferencedIfPresent(value: Option[_]): AnyRef = {
    value.asInstanceOf[Option[AnyRef]].orNull
  }
}

private class OptionSerializerResolver(config: ScalaModule.Config) extends Serializers.Base {

  private val OPTION = classOf[Option[_]]

  override def findReferenceSerializer(serializationConfig: SerializationConfig,
                                       refType: ReferenceType,
                                       beanDesc: BeanDescription,
                                       formatOverrides: JsonFormat.Value,
                                       contentTypeSerializer: TypeSerializer,
                                       contentValueSerializer: ValueSerializer[AnyRef]): ValueSerializer[_] = {
    if (!OPTION.isAssignableFrom(refType.getRawClass)) return null
    val staticTyping = contentTypeSerializer == null && serializationConfig.isEnabled(
      MapperFeature.USE_STATIC_TYPING
    )
    new OptionSerializer(refType, staticTyping, contentTypeSerializer, contentValueSerializer)
  }
}

trait OptionSerializerModule extends OptionTypeModifierModule {
  override def getModuleName: String = "OptionSerializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    super.getInitializers(config) ++ {
      val builder = new InitializerBuilder()
      builder += new OptionSerializerResolver(config)
      builder.build()
    }
  }
}

object OptionSerializerModule extends OptionSerializerModule
