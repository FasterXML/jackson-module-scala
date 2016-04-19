package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.{ReferenceType, TypeBindings, TypeFactory, TypeModifier}
import com.fasterxml.jackson.module.scala.JacksonModule

private object OptionTypeModifier extends TypeModifier with GenTypeModifier {
  def OPTION = classOf[Option[AnyRef]]

  override def modifyType(typ: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory): JavaType = {
    if (typ.isReferenceType || typ.isContainerType) return typ

    if (classObjectFor(jdkType).exists(OPTION.isAssignableFrom)) {
      ReferenceType.upgradeFrom(typ, typ.containedTypeOrUnknown(0))
    } else typ
  }
}

trait OptionTypeModifierModule extends JacksonModule {
  this += OptionTypeModifier
}
