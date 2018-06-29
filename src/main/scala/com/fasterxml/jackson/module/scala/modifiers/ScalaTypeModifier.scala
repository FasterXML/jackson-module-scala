package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.collection._

class ScalaTypeModifier extends TypeModifier {
  override def modifyType(javaType: JavaType,
                          jdkType: Type,
                          context: TypeBindings,
                          typeFactory: TypeFactory): JavaType = {


    if (javaType.isTypeOrSubTypeOf(classOf[Map[_, _]])) {
      MapLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0), javaType.containedTypeOrUnknown(1))
    } else if (javaType.isTypeOrSubTypeOf(classOf[IterableOnce[_]])) {
      CollectionLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
    } else if (javaType.isTypeOrSubTypeOf(classOf[Option[_]])) {
      if (javaType.isInstanceOf[ReferenceType]) return javaType
      ReferenceType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
    } else if (javaType.isTypeOrSubTypeOf(classOf[Either[_,_]])) {
      // I'm not sure this is the right choice, but it's what the original module does
      ReferenceType.upgradeFrom(javaType, javaType)
    } else {
      javaType
    }
  }
}

trait ScalaTypeModifierModule extends JacksonModule {
  this += new ScalaTypeModifier
}
