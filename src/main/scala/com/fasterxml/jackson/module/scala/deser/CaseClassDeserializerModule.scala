package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.ScalaBeansUtil

import com.fasterxml.jackson.databind.introspect.{AnnotatedField, AnnotatedConstructor, AnnotatedParameter, NopAnnotationIntrospector};

import org.scalastuff.scalabeans.{DeserializablePropertyDescriptor, ConstructorParameter}

private object CaseClassAnnotationIntrospector extends NopAnnotationIntrospector {
  lazy val PRODUCT = classOf[Product]
  lazy val OPTION = classOf[Option[_]]
  lazy val LIST = classOf[List[_]]

  private def maybeIsCaseClass(cls: Class[_]): Boolean = {
    if (!PRODUCT.isAssignableFrom(cls)) false
    else if (OPTION.isAssignableFrom(cls)) false
    else if (LIST.isAssignableFrom(cls)) false
    else if (cls.getName.startsWith("scala.Tuple")) false
    else true
  }

  override def findDeserializationName(af: AnnotatedField): String = {
    val cls = af.getDeclaringClass
    if (!maybeIsCaseClass(cls)) null
    else {
      val properties = ScalaBeansUtil.propertiesOf(cls)

      properties.find {
        case dp: DeserializablePropertyDescriptor => af.getName.equals(dp.name)
        case _ => false
      } map (_.name) getOrElse null
    }
  }

  override def findDeserializationName(param: AnnotatedParameter): String = {
    val cls = param.getDeclaringClass
    if (!maybeIsCaseClass(cls)) null
    else {
      param.getOwner match {
        case _: AnnotatedConstructor => findConstructorParamName(param)
        case _ => null
      }
    }
  }

  private def findConstructorParamName(param: AnnotatedParameter): String = {
    val cls = param.getDeclaringClass
    if (!maybeIsCaseClass(cls)) null
    else {
      val properties = ScalaBeansUtil.propertiesOf(cls)

      properties.find {
        case cp: ConstructorParameter => cp.index == param.getIndex
        case _ => false
      }.map(_.name) getOrElse null
    }
  }

}

trait CaseClassDeserializerModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(CaseClassAnnotationIntrospector) }
}