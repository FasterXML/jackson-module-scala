package com.fasterxml.jackson.module.scala
package introspect

import java.lang.reflect.{AccessibleObject, Constructor, Field, Method}

import com.fasterxml.jackson.module.scala.util.Implicits._

import scala.language.existentials

case class ConstructorParameter(constructor: Constructor[_], index: Int, defaultValue: Option[() => AnyRef])

case class PropertyDescriptor(name: String,
                              param: Option[ConstructorParameter],
                              field: Option[Field],
                              getter: Option[Method],
                              setter: Option[Method],
                              beanGetter: Option[Method],
                              beanSetter: Option[Method])
{
  if (List(field, getter).flatten.isEmpty) throw new IllegalArgumentException("One of field or getter must be defined.")

  def findAnnotation[A <: java.lang.annotation.Annotation](implicit mf: Manifest[A]): Option[A] = {
    val cls = mf.runtimeClass.asInstanceOf[Class[A]]
    lazy val paramAnnotation = (param flatMap { cp =>
      val paramAnnos = cp.constructor.getParameterAnnotations
      paramAnnos(cp.index).find(cls.isInstance)
    }).asInstanceOf[Option[A]]
    val getAnno = (o: AccessibleObject) => o.getAnnotation(cls)
    lazy val fieldAnnotation = field optMap getAnno
    lazy val getterAnnotation = getter optMap getAnno
    lazy val beanGetterAnnotation = beanGetter optMap getAnno

    paramAnnotation orElse fieldAnnotation orElse getterAnnotation orElse beanGetterAnnotation
  }

}
