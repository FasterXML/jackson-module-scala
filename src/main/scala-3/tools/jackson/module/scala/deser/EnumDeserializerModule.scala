package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.deser.{Deserializers, KeyDeserializers}
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.Scala3EnumInfo

import java.lang.reflect.InvocationTargetException
import scala.reflect.Enum
import scala.util.Try

private[scala] object EnumDeserializerShared {
  val IntClass = classOf[Int]
  val StringClass = classOf[String]
  val EnumClass = classOf[Enum]

  def tryValueOf(clz: Class[_], key: String): Option[_] = {
    Try(clz.getMethod("valueOf", EnumDeserializerShared.StringClass)).toOption.map { method =>
      method.invoke(None.orNull, key)
    }
  }

  // if any of the enum cases is parameterized then Scala does not support fromOrdinal
  def canFindByOrdinal(clz: Class[_]): Boolean = {
    Try(clz.getMethod("fromOrdinal", IntClass)).toOption.map { method =>
      try {
        method.invoke(None.orNull, 0) != null
      } catch {
        case _ => false
      }
    }.getOrElse(false)
  }

  def matchBasedOnOrdinal(clz: Class[_], key: String): Option[_] = {
    Try(clz.getMethod("fromOrdinal", IntClass)).toOption.flatMap { method =>
      var i = 0
      var matched: Option[_] = None
      var complete = false
      while (!complete) {
        try {
          val enumValue = method.invoke(None.orNull, i)
          if (enumValue.toString == key) {
            matched = Some(enumValue)
            complete = true
          }
        } catch {
          case _: NoSuchElementException => {
            matched = None
            complete = true
          }
          case itex: InvocationTargetException => {
            Option(itex.getCause) match {
              case Some(e) if e.isInstanceOf[NoSuchElementException] => {
                matched = None
                complete = true
              }
              case Some(e) => throw e
              case _ => throw itex
            }
          }
        }
        i += 1
      }
      matched
    }
  }

  // Scala 3 enums that mix parameterized and simple cases have neither valueOf nor a usable
  // fromOrdinal, so they are handled from the reflective case table instead.
  def taggedSumInfo(clz: Class[_]): Option[Scala3EnumInfo.Info] =
    Scala3EnumInfo.infoFor(clz).filter(_.isTaggedSum)
}

private case class EnumDeserializer[T <: Enum](clazz: Class[T]) extends StdDeserializer[T](clazz) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): T = {
    val result = Option(p.getValueAsString).flatMap { text =>
      Try {
        EnumDeserializerShared.tryValueOf(clazz, text)
          .orElse(EnumDeserializerShared.matchBasedOnOrdinal(clazz, text))
      }.toOption.flatten
    }
    result.getOrElse(throw new IllegalArgumentException(s"Failed to create Enum instance for ${p.getValueAsString}"))
      .asInstanceOf[T]
  }
}

/**
 * Deserializer for Scala 3 enums that have parameterized cases. Simple cases are read from their
 * name (as written by the serializer), parameterized cases from a JSON object tagged with a
 * `type` property naming the case.
 */
private case class Scala3EnumSumDeserializer[T <: Enum](info: Scala3EnumInfo.Info)
  extends StdDeserializer[T](info.rootClass) {

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): T = {
    val value = p.currentToken() match {
      case JsonToken.START_OBJECT => fromObject(p, ctxt)
      case _ => fromName(p.getValueAsString)
    }
    value.asInstanceOf[T]
  }

  private def fromName(name: String): AnyRef = {
    if (name == null) failed(name)
    else info.caseForName(name).flatMap(_.singleton).getOrElse(failed(name))
  }

  private def fromObject(p: JsonParser, ctxt: DeserializationContext): AnyRef = {
    val buffer = ctxt.bufferForInputBuffering(p)
    buffer.writeStartObject()
    var typeId: String = None.orNull
    while (p.nextToken() != JsonToken.END_OBJECT) {
      val name = p.currentName()
      p.nextToken()
      if (typeId == null && name == Scala3EnumInfo.TypePropertyName) {
        typeId = p.getValueAsString
      } else {
        buffer.writeName(name)
        buffer.copyCurrentStructure(p)
      }
    }
    buffer.writeEndObject()
    val enumCase = Option(typeId).flatMap(info.caseForName).getOrElse(failed(typeId))
    enumCase.singleton match {
      case Some(singleton) => singleton
      case None => ctxt.readValue(buffer.asParserOnFirstToken(ctxt), enumCase.clazz.asInstanceOf[Class[AnyRef]])
    }
  }

  private def failed(name: String): Nothing =
    throw new IllegalArgumentException(s"Failed to create ${info.rootClass.getName} instance for $name")
}

private case class EnumKeyDeserializer[T <: Enum](clazz: Class[T]) extends KeyDeserializer {
  override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef = {
    val result = Try {
      EnumDeserializerShared.tryValueOf(clazz, key)
        .orElse(EnumDeserializerShared.matchBasedOnOrdinal(clazz, key))
    }.toOption.flatten
    val enumResult = result.getOrElse(throw new IllegalArgumentException(s"Failed to create Enum instance for $key"))
    enumResult.asInstanceOf[AnyRef]
  }
}

private class EnumDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[Enum] =
    deserializerFor(javaType.getRawClass).orNull

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean =
    deserializerFor(valueType).isDefined

  private def deserializerFor(rawClass: Class[_]): Option[ValueDeserializer[Enum]] = {
    if (!EnumDeserializerShared.EnumClass.isAssignableFrom(rawClass)) None
    else EnumDeserializerShared.taggedSumInfo(rawClass) match {
      // the generated case classes are left to the standard bean deserializer
      case Some(info) => if (info.rootClass == rawClass) Some(Scala3EnumSumDeserializer(info)) else None
      case None =>
        if (EnumDeserializerShared.canFindByOrdinal(rawClass)) Some(EnumDeserializer(rawClass.asInstanceOf[Class[Enum]]))
        else None
    }
  }
}

private class EnumKeyDeserializerResolver(config: ScalaModule.Config) extends KeyDeserializers {
  override def findKeyDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier): KeyDeserializer =
    if (EnumDeserializerShared.EnumClass.isAssignableFrom(javaType.getRawClass))
      EnumKeyDeserializer(javaType.getRawClass.asInstanceOf[Class[Enum]])
    else None.orNull
}

trait EnumDeserializerModule extends JacksonModule {
  override def getModuleName: String = "EnumDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumDeserializerResolver(config)
    builder += new EnumKeyDeserializerResolver(config)
    builder.build()
  }
}

object EnumDeserializerModule extends EnumDeserializerModule
