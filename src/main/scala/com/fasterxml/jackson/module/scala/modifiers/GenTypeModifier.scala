package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.{ParameterizedType, Type}

private [modifiers] trait GenTypeModifier {

  protected def classObjectFor(jdkType: Type) = jdkType match {
    case cls: Class[_] => Some(cls)
    case pt: ParameterizedType => pt.getRawType match {
      case cls: Class[_] => Some(cls)
      case _ => None
    }
    case _ => None
  }

}