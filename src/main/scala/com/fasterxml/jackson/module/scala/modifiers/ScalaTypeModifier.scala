package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.module.scala.JacksonModule

import java.lang.reflect.Type
import scala.collection._
import scala.collection.immutable.IntMap

class ScalaTypeModifier extends TypeModifier {

  private val optionClass = classOf[Option[_]]
  private val eitherClass = classOf[Either[_, _]]
  private val mapClass = classOf[Map[_, _]]
  private val intClass = classOf[Int]
  private val intMapClass = classOf[IntMap[_]]
  private val longClass = classOf[Long]
  private val immutableLongMapClass = classOf[immutable.LongMap[_]]
  private val mutableLongMapClass = classOf[mutable.LongMap[_]]
  private val iterableOnceClass = classOf[IterableOnce[_]]

  override def modifyType(javaType: JavaType,
                          jdkType: Type,
                          context: TypeBindings,
                          typeFactory: TypeFactory): JavaType = {


    if (javaType.isTypeOrSubTypeOf(optionClass)) {
      javaType match {
        case rt: ReferenceType => rt
        case _ => ReferenceType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0))
      }
    } else if (javaType.isTypeOrSubTypeOf(mapClass)) {
      if (javaType.isTypeOrSubTypeOf(intMapClass)) {
        MapLikeType.upgradeFrom(javaType, typeFactory.constructType(intClass), javaType.containedTypeOrUnknown(0))
      } else if (javaType.isTypeOrSubTypeOf(immutableLongMapClass) || javaType.isTypeOrSubTypeOf(mutableLongMapClass)) {
        MapLikeType.upgradeFrom(javaType, typeFactory.constructType(longClass), javaType.containedTypeOrUnknown(0))
      } else {
        MapLikeType.upgradeFrom(javaType, javaType.containedTypeOrUnknown(0), javaType.containedTypeOrUnknown(1))
      }
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
  override def getModuleName: String = "ScalaTypeModifierModule"
  this += new ScalaTypeModifier
}