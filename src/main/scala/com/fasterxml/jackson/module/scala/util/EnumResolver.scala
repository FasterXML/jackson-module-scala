package com.fasterxml.jackson.module.scala
package util

import java.lang.reflect.ParameterizedType

import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector

object EnumResolver {

  def apply(property: BeanProperty): Option[EnumResolver] = {
    Option(property)
      .flatMap(p => ScalaAnnotationIntrospector.propertyFor(p.getMember))
      .flatMap(_.findAnnotation[JsonScalaEnumeration])
      .map(a => apply(a))
  }

  def apply(a: JsonScalaEnumeration): EnumResolver = {
    val pt = a.value().getGenericSuperclass.asInstanceOf[ParameterizedType]
    val args = pt.getActualTypeArguments
    apply(args(0).asInstanceOf[Class[Enumeration]])
  }

  def apply[T <: Enumeration](cls: Class[T]): EnumResolver = {
    val enum = cls.getField("MODULE$").get(null).asInstanceOf[T]
    apply(enum)
  }

  def apply(e: Enumeration): EnumResolver = {
    val valueSet = e.values
    val map: Map[String, e.type#Value] = valueSet.iterator.map(v => (v.toString, v)).toMap
    new EnumResolver(e.getClass, valueSet, map)
  }
}

class EnumResolver(cls: Class[_], valueSet: Enumeration#ValueSet, enumsByName: Map[String, Enumeration#Value]) {

  def getEnum(key: String): Enumeration#Value = enumsByName(key)

  def getEnumClass = cls

}
