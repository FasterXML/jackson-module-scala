package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.`type`.{TypeFactory, TypeBindings, SimpleType, TypeModifier}
import java.lang.reflect.{ParameterizedType, Type}

object SeqTypeModifier extends TypeModifier {
  private val BASE = classOf[Seq[_]]
  // Workaround for http://jira.codehaus.org/browse/JACKSON-638
  private def UNKNOWN = SimpleType.construct(classOf[AnyRef])

  def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) =
    classObjectFor(jdkType) map { cls =>
      if (BASE.isAssignableFrom(cls)) {
        val eltType = if (originalType.containedTypeCount() == 1) originalType.containedType(0) else UNKNOWN
        typeFactory.constructCollectionLikeType(cls, eltType)
      }
      else
        originalType
    } getOrElse originalType

  private def classObjectFor(jdkType: Type) = jdkType match {
    case cls: Class[_] => Some(cls)
    case pt: ParameterizedType => pt.getRawType match {
      case cls: Class[_] => Some(cls)
      case _ => None
    }
    case _ => None
  }
}

trait SeqTypeModifierModule {
  self: JacksonModule =>

  this += SeqTypeModifier
}