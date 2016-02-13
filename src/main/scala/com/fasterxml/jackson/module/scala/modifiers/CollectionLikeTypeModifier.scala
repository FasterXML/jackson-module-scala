package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.{CollectionLikeType, TypeBindings, TypeFactory, TypeModifier}

private [modifiers] trait CollectionLikeTypeModifier extends TypeModifier with GenTypeModifier {

  def BASE: Class[_]

  override def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) = {
    if (classObjectFor(jdkType).exists(BASE.isAssignableFrom) && !originalType.isMapLikeType && originalType.containedTypeCount <= 1) {
      val valType = originalType.containedTypeOrUnknown(0)
      CollectionLikeType.upgradeFrom(originalType, valType)
    } else originalType
  }

}