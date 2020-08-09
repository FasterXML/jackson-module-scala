package com.fasterxml.jackson.module.scala.modifiers

import java.lang.reflect.Type

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.module.scala.JacksonModule

class ScalaTypeModifier extends TypeModifier {

  private val optionClass = classOf[Option[_]]
  private val eitherClass = classOf[Either[_, _]]
  private val mapClass = classOf[scala.collection.Map[_, _]]
  private val iterableOnceClass = classOf[scala.collection.IterableOnce[_]]

  override def modifyType(javaType: JavaType,
                          jdkType: Type,
                          context: TypeBindings,
                          typeFactory: TypeFactory): JavaType = {


    if (javaType.isTypeOrSubTypeOf(optionClass)) {
      if (javaType.isInstanceOf[ReferenceType]) return javaType
      ReferenceType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
    } else if (javaType.isTypeOrSubTypeOf(mapClass)) {
      MapLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0), javaType.containedTypeOrUnknown(1))
    } else if (javaType.isTypeOrSubTypeOf(iterableOnceClass)) {
      CollectionLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
    } else if (javaType.isTypeOrSubTypeOf(eitherClass)) {
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