package com.fasterxml.jackson.module.scala
package introspect

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.`type`.ClassKey
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.LRUMap
import com.fasterxml.jackson.module.scala.util.Implicits._

import scala.collection.JavaConverters._

object ScalaAnnotationIntrospector extends JacksonAnnotationIntrospector
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

  private def annotatedClassFor(am: AnnotatedMember): AnnotatedClass =
    am.getContextClass

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

  private def paramFor(a: Annotated): Option[AnnotatedParameter] = {
    a match {
      case am: AnnotatedMember =>
        val d = _descriptorFor(am.getDeclaringClass)
        val prop = d.properties.find(p =>
          (p.field ++ p.getter ++ p.setter ++ p.param).exists(_  == a.getAnnotated)
        )
        prop.flatMap(_.param).flatMap { cp =>
          annotatedClassFor(am)
            .getConstructors.asScala.find(_.getAnnotated == cp.constructor)
            .map(_.getParameter(cp.index))
        }
      case _ => None
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

  override def findNameForSerialization(member: Annotated): PropertyName =
    paramFor(member).flatMap(p => Option(super.findNameForSerialization(p))).orNull

  override def hasCreatorAnnotation(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedConstructor =>
        val d = _descriptorFor(ac.getDeclaringClass)
        d.properties.exists(p => p.param.exists(_.constructor == ac.getAnnotated))
      case _ => false
    }
  }

  override def findCreatorBinding(a: Annotated): JsonCreator.Mode =
    paramFor(a) optMap { _.getAnnotation(classOf[JsonCreator]) } map { _.mode } getOrElse {
      if (hasCreatorAnnotation(a)) JsonCreator.Mode.PROPERTIES else null
    }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
}
