package com.fasterxml.jackson.module.scala.modifiers

import org.codehaus.jackson.`type`.JavaType
import java.lang.reflect.Type
import org.codehaus.jackson.map.`type`.{TypeFactory, TypeBindings, TypeModifier}
import com.fasterxml.jackson.module.scala.JacksonModule

private object MapTypeModifer extends TypeModifier with GenTypeModifier {

  val BASE = classOf[collection.Map[_,_]]

  override def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) =
    classObjectFor(jdkType) find (BASE.isAssignableFrom(_)) map { cls =>
      val keyType = if (originalType.containedTypeCount() >= 1) originalType.containedType(0) else UNKNOWN
      val valueType = if (originalType.containedTypeCount() >= 2) originalType.containedType(1) else UNKNOWN
      typeFactory.constructMapLikeType(cls, keyType, valueType)
    } getOrElse originalType

}

/**
 * @author Christopher Currie <ccurrie@impresys.com>
 */
trait MapTypeModifierModule extends JacksonModule {
  this += MapTypeModifer
}