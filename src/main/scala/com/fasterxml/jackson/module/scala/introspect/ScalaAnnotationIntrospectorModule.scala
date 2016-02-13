package com.fasterxml.jackson
package module
package scala
package introspect

import annotation.JsonCreator
import databind.`type`.ClassKey
import databind.introspect._
import databind.util.LRUMap
import paranamer.ParanamerAnnotationIntrospector

import util.Implicits._

object ScalaAnnotationIntrospector extends NopAnnotationIntrospector
{
  private [this] val _descriptorCache = new LRUMap[ClassKey, BeanDescriptor](16, 100)

  private def _descriptorFor(clz: Class[_]): BeanDescriptor = {
    val key = new ClassKey(clz)
    var result = _descriptorCache.get(key)
    if (result == null) {
      result = BeanIntrospector(clz)
      _descriptorCache.put(key, result)
    }

    result
  }

  private def fieldName(af: AnnotatedField): Option[String] = {
    val d = _descriptorFor(af.getDeclaringClass)
    d.properties.find(p => p.field.exists(_ == af.getAnnotated)).map(_.name)
  }

  private def methodName(am: AnnotatedMethod): Option[String] = {
    val d = _descriptorFor(am.getDeclaringClass)
    d.properties.find(p => (p.getter ++ p.setter).exists(_ == am.getAnnotated)).map(_.name)
  }

  private def paramName(ap: AnnotatedParameter): Option[String] = {
    val d = _descriptorFor(ap.getDeclaringClass)
    d.properties.find(p => p.param.exists { cp =>
      cp.constructor == ap.getOwner.getAnnotated && cp.index == ap.getIndex
    }).map(_.name)
  }

  private def isScalaPackage(pkg: Option[Package]): Boolean =
    pkg.exists(_.getName.startsWith("scala."))

  private def isMaybeScalaBeanType(cls: Class[_]): Boolean =
    cls.hasSignature && !isScalaPackage(Option(cls.getPackage))

  private def isScala(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedClass => isMaybeScalaBeanType(ac.getAnnotated)
      case am: AnnotatedMember => isMaybeScalaBeanType(am.getDeclaringClass)
    }
  }

  def propertyFor(a: Annotated): Option[PropertyDescriptor] = {
    a match {
      case ap: AnnotatedParameter =>
        val d = _descriptorFor(ap.getDeclaringClass)
        d.properties.find(p =>
          p.param.exists { cp =>
            (cp.constructor == ap.getOwner.getAnnotated) && (cp.index == ap.getIndex)
          }
        )
      case am: AnnotatedMember =>
        val d = _descriptorFor(am.getDeclaringClass)
        d.properties.find(p =>
          (p.field ++ p.getter ++ p.setter ++ p.param ++ p.beanGetter ++ p.beanSetter).exists(_  == a.getAnnotated)
        )
    }
  }

  override def findImplicitPropertyName(member: AnnotatedMember): String = {
    member match {
      case af: AnnotatedField => fieldName(af).orNull
      case am: AnnotatedMethod => methodName(am).orNull
      case ap: AnnotatedParameter => paramName(ap).orNull
      case _ => null
    }
  }

  override def hasCreatorAnnotation(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedConstructor =>
        isScala(ac) && _descriptorFor(ac.getDeclaringClass).
          properties.view.flatMap(_.param).exists(_.constructor == ac.getAnnotated)
      case _ => false
    }
  }

  override def findCreatorBinding(a: Annotated): JsonCreator.Mode =
    if (isScala(a) && hasCreatorAnnotation(a)) JsonCreator.Mode.PROPERTIES else null

}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(new ParanamerAnnotationIntrospector()) }
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
}
