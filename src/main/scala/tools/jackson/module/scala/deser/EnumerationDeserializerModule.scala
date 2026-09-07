package tools.jackson.module.scala
package deser

import tools.jackson.core.{JsonParser, JsonToken}
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind._
import tools.jackson.databind.deser.{ContextualKeyDeserializer, Deserializers, KeyDeserializers}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.util.EnumResolver
import tools.jackson.module.scala.{JacksonModule => JacksonScalaModule}

import scala.util.control.NonFatal

private trait ContextualEnumerationDeserializer {
  self: ValueDeserializer[Enumeration#Value] =>

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) : ValueDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer = {
    EnumResolver(ctxt.getContextualType, property).map(r => new AnnotatedEnumerationDeserializer(r)).getOrElse(this)
  }

}

/**
 * This class is mostly legacy logic to be deprecated/removed in 3.0
 */
private class EnumerationDeserializer(theType: JavaType) extends ValueDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
  override def deserialize(jp:JsonParser, ctxt:DeserializationContext): Enumeration#Value = {
    if (jp.currentToken() != JsonToken.START_OBJECT) {
      ctxt.handleUnexpectedToken(theType, jp).asInstanceOf[Enumeration#Value]
    } else {
      val (eclass, eclassName) = parsePair(jp)
      if (eclass != "enumClass") {
        ctxt.handleUnexpectedToken(theType, jp).asInstanceOf[Enumeration#Value]
      } else {
        val (value, valueValue) = parsePair(jp)
        if (value != "value") {
          ctxt.handleUnexpectedToken(theType, jp).asInstanceOf[Enumeration#Value]
        } else {
          jp.nextToken()
          enumerationValue(eclassName, valueValue, ctxt)
        }
      }
    }
  }

  /**
   * The value `valueName` of the Enumeration named by `enumClassName`.
   *
   * The JSON names the class, so both halves of this fail on input a service does not control: the
   * class may not be there, and the name may not be one of its values. Either is a databind problem
   * and is reported as one, rather than letting a raw ClassNotFoundException or NoSuchElementException
   * out of readValue past everything catching JacksonException.
   */
  private def enumerationValue(enumClassName: String, valueName: String, ctxt: DeserializationContext): Enumeration#Value = {
    val enumeration =
      try {
        // the mapper's loader first: this module's own is the wrong one wherever the application's
        // classes are loaded by a child loader, which is where this used to fail to find them
        val loader = Option(ctxt.getTypeFactory.getClassLoader).getOrElse(getClass.getClassLoader)
        Class.forName(enumClassName + "$", false, loader)
          .getField("MODULE$").get(None.orNull).asInstanceOf[Enumeration]
      } catch {
        case NonFatal(e) =>
          return ctxt.handleInstantiationProblem(classOf[Enumeration], enumClassName, e)
            .asInstanceOf[Enumeration#Value]
      }
    try enumeration.withName(valueName)
    catch {
      case NonFatal(_) =>
        ctxt.handleWeirdStringValue(enumeration.getClass, valueName,
          s"not a value of Enumeration $enumClassName").asInstanceOf[Enumeration#Value]
    }
  }

  private def parsePair(jp: JsonParser): (String, String) = (nextToken(jp), nextToken(jp))
  private def nextToken(jp: JsonParser): String = {
    jp.nextToken
    jp.getString
  }
}

private class AnnotatedEnumerationDeserializer(r: EnumResolver) extends ValueDeserializer[Enumeration#Value] with ContextualEnumerationDeserializer {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Enumeration#Value = {
    jp.currentToken() match {
      case JsonToken.VALUE_STRING => r.getEnum(jp.getValueAsString)
      case _ => ctxt.handleUnexpectedToken(r.getJavaType, jp).asInstanceOf[Enumeration#Value]
    }
  }
}

private class EnumerationDeserializerResolver(config: ScalaModule.Config) extends Deserializers.Base {

  private val ENUMERATION = classOf[scala.Enumeration#Value]

  override def findBeanDeserializer(javaType: JavaType,
                                    deserializationConfig: DeserializationConfig,
                                    beanDesc: BeanDescription.Supplier) = {

    val clazz = javaType.getRawClass
    if (ENUMERATION.isAssignableFrom(clazz)) {
      new EnumerationDeserializer(javaType)
    } else {
      None.orNull
    }
  }

  override def hasDeserializerFor(deserializationConfig: DeserializationConfig, valueType: Class[_]): Boolean = {
    ENUMERATION.isAssignableFrom(valueType)
  }
}

private class EnumerationKeyDeserializer(r: Option[EnumResolver]) extends KeyDeserializer with ContextualKeyDeserializer {

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) = {
    val newResolver = EnumResolver(ctxt.getContextualType, property)
    if (newResolver != r) new EnumerationKeyDeserializer(newResolver) else this
  }

  def deserializeKey(s: String, ctxt: DeserializationContext): Enumeration#Value = {
    r match {
      case Some(resolved) => resolved.getEnum(s)
      case _ => throw DatabindException.from(ctxt, "Need @JsonScalaEnumeration to determine key type")
    }
  }
}

private class EnumerationKeyDeserializers(config: ScalaModule.Config) extends KeyDeserializers {
  private val valueClass = classOf[scala.Enumeration#Value]
  def findKeyDeserializer(tp: JavaType, deserializationConfig: DeserializationConfig, desc: BeanDescription.Supplier): KeyDeserializer = {
    if (valueClass.isAssignableFrom(tp.getRawClass)) {
      new EnumerationKeyDeserializer(None)
    }
    else None.orNull
  }
}

trait EnumerationDeserializerModule extends JacksonScalaModule {
  override def getModuleName: String = "EnumerationDeserializerModule"

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += { ctxt =>
      ctxt.addDeserializers(new EnumerationDeserializerResolver(config))
      ctxt.addKeyDeserializers(new EnumerationKeyDeserializers(config))
    }
    builder.build()
  }
}

object EnumerationDeserializerModule extends EnumerationDeserializerModule
