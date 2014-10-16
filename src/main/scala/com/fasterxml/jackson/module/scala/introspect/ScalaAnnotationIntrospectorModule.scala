package com.fasterxml.jackson.module.scala
package introspect

import com.fasterxml.jackson.databind.PropertyName
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.module.scala.util.Implicits._
import com.google.common.cache.{CacheBuilder, LoadingCache}

import scala.collection.JavaConverters._

object ScalaAnnotationIntrospector extends JacksonAnnotationIntrospector
{
  val descriptors: LoadingCache[Class[_], BeanDescriptor] =
    CacheBuilder.newBuilder()
      .maximumSize(DEFAULT_CACHE_SIZE)
      .build(BeanIntrospector.apply (_:Class[_]))

  private def fieldName(af: AnnotatedField): Option[String] = {
    val d = descriptors(af.getDeclaringClass)
    d.properties.find(p => p.field.exists(_ == af.getAnnotated)).map(_.name)
  }

  private def methodName(am: AnnotatedMethod): Option[String] = {
    val d = descriptors(am.getDeclaringClass)
    d.properties.find(p => (p.getter ++ p.setter).exists(_ == am.getAnnotated)).map(_.name)
  }

  private def paramName(ap: AnnotatedParameter): Option[String] = {
    val d = descriptors(ap.getDeclaringClass)
    d.properties.find(p => p.param.exists { cp =>
      cp.constructor == ap.getOwner.getAnnotated && cp.index == ap.getIndex
    }).map(_.name)
  }

  private def paramFor(a: Annotated): Option[AnnotatedParameter] = {
    a match {
      case am: AnnotatedMember =>
        val d = descriptors(am.getDeclaringClass)
        val prop = d.properties.find(p =>
          (p.field ++ p.getter ++ p.setter ++ p.param).exists(_  == a.getAnnotated)
        )
        prop.flatMap(_.param).flatMap { cp =>
          AnnotatedClass.constructWithoutSuperTypes(am.getDeclaringClass, this, null)
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

  override def findNameForSerialization(member: Annotated): PropertyName = {
    paramFor(member).map(super.findNameForSerialization).orNull
  }

  override def findNameForDeserialization(member: Annotated): PropertyName = {
    paramFor(member).map(super.findNameForDeserialization).orNull
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
}
