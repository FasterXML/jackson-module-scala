package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.`type`.{TypeBindings, TypeFactory, TypeModifier};

private [modifiers] trait CollectionLikeTypeModifier extends TypeModifier with GenTypeModifier {

  def BASE: Class[_]

  override def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) =
    if (originalType.containedTypeCount() > 1) originalType else
    classObjectFor(jdkType) find (BASE.isAssignableFrom(_)) map { cls =>
      val eltType = if (originalType.containedTypeCount() == 1) originalType.containedType(0) else UNKNOWN
      typeFactory.constructCollectionLikeType(cls, eltType)
    } getOrElse originalType

}