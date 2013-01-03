package com.fasterxml.jackson.module.scala.deser

import com.fasterxml.jackson.module.scala.JacksonModule

import com.fasterxml.jackson.databind.introspect.{AnnotatedField, AnnotatedConstructor, AnnotatedParameter, NopAnnotationIntrospector}
import com.fasterxml.jackson.module.scala.introspect.{PropertyDescriptor, BeanIntrospector}
;

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
      val properties = BeanIntrospector(cls).properties
      (properties.collect {
        case PropertyDescriptor(name, _, Some(f), _, _) if f equals af.getAnnotated => name
      }).headOption.orNull
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
      val properties = BeanIntrospector(cls).properties
      (properties.collect {
        case PropertyDescriptor(name, Some(p), _, _, _)
          if (p.constructor equals param.getOwner.getAnnotated) && (p.index equals param.getIndex)
          => name
      }).headOption.orNull
    }
  }

}

trait CaseClassDeserializerModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(CaseClassAnnotationIntrospector) }
}