package com.fasterxml.jackson.module.scala.introspect

import java.lang.reflect.{Field, Method, Constructor}
import reflect.NameTransformer

case class ConstructorParameter(constructor: Constructor[_], index: Int, defaultValueMethod: Option[Method])

case class PropertyDescriptor(name: String,
                              param: Option[ConstructorParameter],
                              field: Option[Field],
                              getter: Option[Method],
                              setter: Option[Method])
{
  if (List(field, getter).flatten.isEmpty) throw new IllegalArgumentException("One of field or getter must be defined.")

  def findAnnotation[A <: java.lang.annotation.Annotation](implicit mf: Manifest[A]): Option[A] = {
    val cls = mf.erasure.asInstanceOf[Class[A]]
    lazy val paramAnnotation = param flatMap { cp =>
      cp.constructor.getParameterAnnotations.apply(cp.index).find(_.getClass equals cls)
    }
    lazy val fieldAnnotation = field flatMap { f => Option(f.getAnnotation(cls)) }
    lazy val getterAnnotation = setter flatMap { f => Option(f.getAnnotation(cls)) }

    (paramAnnotation orElse fieldAnnotation orElse getterAnnotation).asInstanceOf[Option[A]]
  }

}