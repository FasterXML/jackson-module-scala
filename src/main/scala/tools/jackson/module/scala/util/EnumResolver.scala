package tools.jackson.module.scala
package util

import tools.jackson.databind.{BeanProperty, JavaType}

import java.lang.reflect.ParameterizedType

object EnumResolver {

  def apply(javaType: JavaType, property: BeanProperty): Option[EnumResolver] = {
    Option(property)
      .flatMap(p => Option(p.getAnnotation(classOf[JsonScalaEnumeration])))
      .map(a => apply(javaType, a))
  }

  def apply(javaType: JavaType, annotation: JsonScalaEnumeration): EnumResolver = {
    val pt = annotation.value().getGenericSuperclass.asInstanceOf[ParameterizedType]
    val args = pt.getActualTypeArguments
    apply(javaType, args(0).asInstanceOf[Class[Enumeration]])
  }

  def apply[T <: Enumeration](javaType: JavaType, cls: Class[T]): EnumResolver = {
    val enumInstance = cls.getField("MODULE$").get(null).asInstanceOf[T]
    apply(javaType, enumInstance)
  }

  def apply(javaType: JavaType, e: Enumeration): EnumResolver = {
    val valueSet = e.values
    val map: Map[String, e.type#Value] = valueSet.iterator.map(v => (v.toString, v)).toMap
    new EnumResolver(javaType, valueSet, map)
  }
}

class EnumResolver(javaType: JavaType, valueSet: Enumeration#ValueSet, enumsByName: Map[String, Enumeration#Value]) {

  def getEnum(key: String): Enumeration#Value = enumsByName(key)

  def getJavaType: JavaType = javaType
}
