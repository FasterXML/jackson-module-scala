package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.`type`.ClassKey
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.LRUMap
import com.fasterxml.jackson.module.paranamer.ParanamerAnnotationIntrospector
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicits._

import java.lang.annotation.Annotation

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
    val jsonCreators: PartialFunction[Annotation, JsonCreator] = { case jc: JsonCreator => jc }

    a match {
      case ac: AnnotatedConstructor =>
        if (!isScala(ac)) return false
        val annotatedFound = _descriptorFor(ac.getDeclaringClass)
          .properties
          .flatMap(_.param)
          .exists(_.constructor == ac.getAnnotated)

        // Ignore this annotation if there is another annotation that is actually annotated with @JsonCreator.
        val annotatedConstructor = {
          for (constructor <- ac.getDeclaringClass.getDeclaredConstructors;
               annotation: JsonCreator <- constructor.getAnnotations.collect(jsonCreators) if annotation.mode() != JsonCreator.Mode.DISABLED) yield constructor
        }.headOption

        // Ignore this annotation if it is Mode.DISABLED.
        val isDisabled = ac.getAnnotated.getAnnotations.collect(jsonCreators).exists(_.mode() == JsonCreator.Mode.DISABLED)

        annotatedFound && annotatedConstructor.forall(_ == ac.getAnnotated) && !isDisabled
      case _ => false
    }
  }

  override def findCreatorBinding(a: Annotated): JsonCreator.Mode = {
    val ann = _findAnnotation(a, classOf[JsonCreator])
    if (ann != null) {
      ann.mode()
    } else if (isScala(a) && hasCreatorAnnotation(a)) {
      JsonCreator.Mode.PROPERTIES
    } else null
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(new ParanamerAnnotationIntrospector()) }
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
}
