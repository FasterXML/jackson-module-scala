package com.fasterxml.jackson.module.scala.introspect

import java.lang.annotation.Annotation
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.`type`.ClassKey
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator
import com.fasterxml.jackson.databind.deser._
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.{AccessPattern, LRUMap, LookupCache}
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext}
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicits._

object ScalaAnnotationIntrospector extends NopAnnotationIntrospector with ValueInstantiators {
  private [this] var _descriptorCache: LookupCache[ClassKey, BeanDescriptor] =
    new LRUMap[ClassKey, BeanDescriptor](16, 100)

  def setDescriptorCache(cache: LookupCache[ClassKey, BeanDescriptor]): LookupCache[ClassKey, BeanDescriptor] = {
    val existingCache = _descriptorCache
    _descriptorCache = cache
    existingCache
  }

  def propertyFor(a: Annotated): Option[PropertyDescriptor] = {
    a match {
      case ap: AnnotatedParameter =>
        _descriptorFor(ap.getDeclaringClass).flatMap { d =>
          d.properties.find { p =>
            p.param.exists { cp =>
              (cp.constructor == ap.getOwner.getAnnotated) && (cp.index == ap.getIndex)
            }
          }
        }
      case am: AnnotatedMember =>
        _descriptorFor(am.getDeclaringClass).flatMap { d =>
          d.properties.find { p =>
            (p.field ++ p.getter ++ p.setter ++ p.param ++ p.beanGetter ++ p.beanSetter).exists(_ == a.getAnnotated)
          }
        }
    }
  }

  override def findImplicitPropertyName(member: AnnotatedMember): String = {
    member match {
      case af: AnnotatedField => fieldName(af).orNull
      case am: AnnotatedMethod => methodName(am).orNull
      case ap: AnnotatedParameter => paramName(ap).orNull
      case _ => None.orNull
    }
  }

  override def hasCreatorAnnotation(a: Annotated): Boolean = {
    val jsonCreators: PartialFunction[Annotation, JsonCreator] = { case jc: JsonCreator => jc }

    a match {
      case ac: AnnotatedConstructor if (!isScala(ac)) => false
      case ac: AnnotatedConstructor =>
        val annotatedFound = _descriptorFor(ac.getDeclaringClass).map { d =>
          d.properties
            .flatMap(_.param)
            .exists(_.constructor == ac.getAnnotated)
        }.getOrElse(false)

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

  override def findCreatorAnnotation(config: MapperConfig[_], a: Annotated): JsonCreator.Mode = {
    if (hasCreatorAnnotation(a)) {
      Option(findCreatorBinding(a)) match {
        case Some(mode) => mode
        case _ => JsonCreator.Mode.DEFAULT
      }
    } else None.orNull
  }

  override def findCreatorBinding(a: Annotated): JsonCreator.Mode = {
    Option(_findAnnotation(a, classOf[JsonCreator])) match {
      case Some(ann) => ann.mode()
      case _ => {
        if (isScala(a) && hasCreatorAnnotation(a)) {
          JsonCreator.Mode.PROPERTIES
        } else None.orNull
      }
    }
  }

  private class ScalaValueInstantiator(delegate: StdValueInstantiator, config: DeserializationConfig, descriptor: BeanDescriptor)
    extends StdValueInstantiator(delegate) {

    private val overriddenConstructorArguments: Array[SettableBeanProperty] = {
      val args = delegate.getFromObjectArguments(config)
      Option(args) match {
        case Some(array) => {
          array.map {
            case creator: CreatorProperty =>
              // Locate the constructor param that matches it
              descriptor.properties.find(_.param.exists(_.index == creator.getCreatorIndex)) match {
                case Some(PropertyDescriptor(name, Some(ConstructorParameter(_, _, Some(defaultValue))), _, _, _, _, _)) =>
                  creator.withNullProvider(new NullValueProvider {
                    override def getNullValue(ctxt: DeserializationContext): AnyRef = defaultValue()

                    override def getNullAccessPattern: AccessPattern = AccessPattern.DYNAMIC
                  })
                case _ => creator
              }
            case other => other
          }
        }
        case _ => Array.empty
      }
    }

    override def getFromObjectArguments(config: DeserializationConfig): Array[SettableBeanProperty] = {
      overriddenConstructorArguments
    }
  }

  override def findValueInstantiator(config: DeserializationConfig, beanDesc: BeanDescription,
                                     defaultInstantiator: ValueInstantiator): ValueInstantiator = {

    if (isMaybeScalaBeanType(beanDesc.getBeanClass)) {

      _descriptorFor(beanDesc.getBeanClass).map { descriptor =>
        if (descriptor.properties.exists(_.param.exists(_.defaultValue.isDefined))) {
          defaultInstantiator match {
            case std: StdValueInstantiator =>
              new ScalaValueInstantiator(std, config, descriptor)
            case other =>
              throw new IllegalArgumentException("Cannot customise a non StdValueInstantiatiator: " + other.getClass)
          }
        } else defaultInstantiator
      }.getOrElse(defaultInstantiator)

    } else defaultInstantiator
  }

  private def _descriptorFor(clz: Class[_]): Option[BeanDescriptor] = {
    if (clz.extendsScalaClass || clz.hasSignature) {
      val key = new ClassKey(clz)
      Option(_descriptorCache.get(key)) match {
        case Some(result) => Some(result)
        case _ => {
          val introspector = BeanIntrospector(clz)
          _descriptorCache.put(key, introspector)
          Some(introspector)
        }
      }
    } else {
      None
    }
  }

  private def fieldName(af: AnnotatedField): Option[String] = {
    _descriptorFor(af.getDeclaringClass).flatMap { d =>
      d.properties.find(p => p.field.exists(_ == af.getAnnotated)).map(_.name)
    }
  }

  private def methodName(am: AnnotatedMethod): Option[String] = {
    _descriptorFor(am.getDeclaringClass).flatMap { d =>
      val getterSetter = d.properties.find(p => (p.getter ++ p.setter).exists(_ == am.getAnnotated)).map(_.name)
      getterSetter match {
        case Some(s) => Some(s)
        case _ => d.properties.find(p => p.name == am.getName).map(_.name)
      }
    }
  }

  private def paramName(ap: AnnotatedParameter): Option[String] = {
    _descriptorFor(ap.getDeclaringClass).flatMap { d =>
      d.properties.find(p => p.param.exists { cp =>
        cp.constructor == ap.getOwner.getAnnotated && cp.index == ap.getIndex
      }).map(_.name)
    }
  }

  private def isScalaPackage(pkg: Option[Package]): Boolean =
    pkg.exists(_.getName.startsWith("scala."))

  private def isMaybeScalaBeanType(cls: Class[_]): Boolean =
    (cls.extendsScalaClass || cls.hasSignature) &&
      !isScalaPackage(Option(cls.getPackage))

  private def isScala(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedClass => isMaybeScalaBeanType(ac.getAnnotated)
      case am: AnnotatedMember => isMaybeScalaBeanType(am.getDeclaringClass)
    }
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(JavaAnnotationIntrospector) }
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
  this += { _.addValueInstantiators(ScalaAnnotationIntrospector) }
}
