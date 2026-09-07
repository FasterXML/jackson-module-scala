package tools.jackson.module.scala.deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.deser.{Deserializers, KeyDeserializers}
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind._
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.module.scala.{JacksonModule, ScalaModule, Scala3EnumSupportState}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.Scala3EnumInfo

import java.lang.reflect.InvocationTargetException
import scala.reflect.Enum
import scala.util.Try
import scala.util.control.NonFatal

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
        // An enum with no case at ordinal 0 answers by throwing, which is the question being asked
        // here. Reflection wraps whatever fromOrdinal threw, so the cause is what decides: a fatal
        // one - an OutOfMemoryError, a thread interrupt - belongs to the caller, not to this
        // question, and arrives wrapped exactly as an ordinary failure does.
        case e: InvocationTargetException => e.getCause match {
          case null => false
          case NonFatal(_) => false
          case fatal => throw fatal
        }
        case NonFatal(_) => false
      }
    }.getOrElse(false)
  }

  /**
   * The case of `clz` whose name is `key`, or `None` where it has none.
   *
   * Read from the enum's own case table. This used to walk the ordinals instead, calling
   * `fromOrdinal(0)`, `fromOrdinal(1)` and so on until one of them threw NoSuchElementException -
   * which cost a reflective call per case on the way to every answer, and ended only if
   * out-of-range was signalled in that one way, leaving an enum that signalled it differently to be
   * walked without bound. `values` is generated alongside `fromOrdinal`, by the same enums, and
   * gives the whole table in a single call that cannot run long or throw at all.
   */
  def matchByName(clz: Class[_], key: String): Option[_] = {
    valuesOf(clz).flatMap(_.find(_.toString == key))
  }

  private def valuesOf(clz: Class[_]): Option[Array[AnyRef]] = {
    Try(clz.getMethod("values")).toOption.flatMap { method =>
      try {
        Option(method.invoke(None.orNull)).map(_.asInstanceOf[Array[AnyRef]])
      } catch {
        case e: InvocationTargetException => e.getCause match {
          case null => None
          case NonFatal(_) => None
          case fatal => throw fatal
        }
        case NonFatal(_) => None
      }
    }
  }

}

private case class EnumDeserializer[T <: Enum](clazz: Class[T]) extends StdDeserializer[T](clazz) {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): T = {
    val result = Option(p.getValueAsString).flatMap { text =>
      Try {
        EnumDeserializerShared.tryValueOf(clazz, text)
          .orElse(EnumDeserializerShared.matchByName(clazz, text))
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
      if (name == Scala3EnumInfo.TypePropertyName) {
        // a second one is refused rather than written on as an ordinary property: which of the two
        // the case was read from would otherwise depend on nothing but their order
        if (typeId != null) {
          ctxt.reportInputMismatch(info.rootClass,
            s"Duplicate ${Scala3EnumInfo.TypePropertyName} property: '$typeId' then '${p.getValueAsString}'")
        }
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
      // the source parser is handed over so that what is read from the buffer still reports where in
      // the input it came from - databind's own AsPropertyTypeDeserializer buffers the same way
      case None => ctxt.readValue(buffer.asParserOnFirstToken(ctxt, p), enumCase.clazz.asInstanceOf[Class[AnyRef]])
    }
  }

  private def failed(name: String): Nothing =
    throw new IllegalArgumentException(s"Failed to create ${info.rootClass.getName} instance for $name")
}

private case class EnumKeyDeserializer[T <: Enum](clazz: Class[T]) extends KeyDeserializer {
  override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef = {
    val result = Try {
      EnumDeserializerShared.tryValueOf(clazz, key)
        .orElse(EnumDeserializerShared.matchByName(clazz, key))
    }.toOption.flatten
    val enumResult = result.getOrElse(throw new IllegalArgumentException(s"Failed to create Enum instance for $key"))
    enumResult.asInstanceOf[AnyRef]
  }
}

private class EnumDeserializerResolver(config: ScalaModule.Config, enumInfo: Scala3EnumInfo) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription.Supplier): ValueDeserializer[Enum] =
    deserializerFor(javaType.getRawClass).orNull

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean =
    deserializerFor(valueType).isDefined

  private def deserializerFor(rawClass: Class[_]): Option[ValueDeserializer[Enum]] = {
    if (!EnumDeserializerShared.EnumClass.isAssignableFrom(rawClass)) None
    else enumInfo.taggedSumInfo(rawClass) match {
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

trait EnumDeserializerModule extends JacksonModule with Scala3EnumSupportState {
  override def getModuleName: String = "EnumDeserializerModule"

  protected def deserializerInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new EnumDeserializerResolver(config, scala3EnumInfo)
    builder += new EnumKeyDeserializerResolver(config)
    builder.build()
  }

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] =
    deserializerInitializers(config)
}

object EnumDeserializerModule extends EnumDeserializerModule
