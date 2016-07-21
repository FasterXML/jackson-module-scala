package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.module.scala.JacksonModule

class ScalaTypeModifier extends TypeModifier {
  override def modifyType(javaType: JavaType,
                          jdkType: Type,
                          context: TypeBindings,
                          typeFactory: TypeFactory): JavaType = {


    if (javaType.isTypeOrSubTypeOf(classOf[collection.Map[_, _]])) {
      MapLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0), javaType.containedTypeOrUnknown(1))
    } else if (javaType.isTypeOrSubTypeOf(classOf[collection.TraversableOnce[_]])) {
      CollectionLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
    } else if (javaType.isTypeOrSubTypeOf(classOf[Option[_]])) {
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
