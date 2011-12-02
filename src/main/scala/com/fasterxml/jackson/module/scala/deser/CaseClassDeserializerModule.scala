package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule
import org.scalastuff.scalabeans.Preamble._
import org.codehaus.jackson.map.introspect.{AnnotatedField, AnnotatedConstructor, AnnotatedParameter, NopAnnotationIntrospector}
import org.scalastuff.scalabeans.{DeserializablePropertyDescriptor, ConstructorParameter}

private object CaseClassAnnotationIntrospector extends NopAnnotationIntrospector {
  private def maybeIsCaseClass(cls: Class[_]): Boolean = {
    if (!classOf[Product].isAssignableFrom(cls)) false
    else if (cls.getName.startsWith("scala.Tuple")) false
    else true
  }

  override def findDeserializablePropertyName(af: AnnotatedField): String = {
    val cls = af.getDeclaringClass
    if (!maybeIsCaseClass(cls)) null
    else {
      val descriptor = descriptorOf(cls)

      descriptor.properties.find {
        case dp: DeserializablePropertyDescriptor => af.getName.equals(dp.name)
        case _ => false
      } map (_.name) getOrElse null
    }
  }

  override def findPropertyNameForParam(param: AnnotatedParameter): String = {
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
      val descriptor = descriptorOf(cls)

      descriptor.properties.find {
        case cp: ConstructorParameter => cp.index == param.getIndex
        case _ => false
      }.map(_.name) getOrElse null
    }
  }

}

trait CaseClassDeserializerModule extends JacksonModule {
  this += { _.insertAnnotationIntrospector(CaseClassAnnotationIntrospector) }
}