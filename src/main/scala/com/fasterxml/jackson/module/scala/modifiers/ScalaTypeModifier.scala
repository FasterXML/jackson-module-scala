package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.databind.JacksonModule.SetupContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.module.scala.JacksonModule.InitializerBuilder
import com.fasterxml.jackson.module.scala.{JacksonModule, ScalaModule}

import java.lang.reflect.Type
import scala.collection._

class ScalaTypeModifier(config: ScalaModule.Config) extends TypeModifier {

  private val optionClass = classOf[Option[_]]
  private val eitherClass = classOf[Either[_, _]]
  private val mapClass = classOf[Map[_, _]]
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
  override def getInitializers(config: ScalaModule.Config): scala.Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    builder += new ScalaTypeModifier(config)
    builder.build()
  }
}