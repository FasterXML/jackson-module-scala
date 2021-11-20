package com.fasterxml.jackson.module.scala.introspect

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.`type`.{ClassKey, CollectionLikeType, MapLikeType, ReferenceType, SimpleType}
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator
import com.fasterxml.jackson.databind.deser._
import com.fasterxml.jackson.databind.introspect._
import com.fasterxml.jackson.databind.util.{AccessPattern, LRUMap, LookupCache}
import com.fasterxml.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext, JavaType, MapperFeature}
import com.fasterxml.jackson.module.scala.JacksonModule
import com.fasterxml.jackson.module.scala.util.Implicits._

import java.lang.annotation.Annotation
import scala.collection.mutable.{Map => MutableMap}

object ScalaAnnotationIntrospector extends NopAnnotationIntrospector with ValueInstantiators {

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

  override def hasIgnoreMarker(m: AnnotatedMember): Boolean = {
    val name = m.getName
    //special cases to prevent shadow fields associated with lazy vals being serialized
    name == "0bitmap$1" || name.endsWith("$lzy1") || super.hasIgnoreMarker(m)
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

  private class ScalaValueInstantiator(scalaAnnotationIntrospectorModule: ScalaAnnotationIntrospectorModule,
                                       delegate: StdValueInstantiator, config: DeserializationConfig, descriptor: BeanDescriptor)
    extends StdValueInstantiator(delegate) {

    private val overriddenConstructorArguments: Array[SettableBeanProperty] = {
      val overrides = scalaAnnotationIntrospectorModule.overrideMap.get(descriptor.beanType)
        .map(_.overrides.toMap).getOrElse(Map.empty)
      val applyDefaultValues = config.isEnabled(MapperFeature.APPLY_DEFAULT_VALUES)
      val args = delegate.getFromObjectArguments(config)
      Option(args) match {
        case Some(array) if (applyDefaultValues || overrides.nonEmpty) => {
          array.map {
            case creator: CreatorProperty => {
              // Locate the constructor param that matches it
              descriptor.properties.find(_.param.exists(_.index == creator.getCreatorIndex)) match {
                case Some(pd) => {
                  val mappedCreator = overrides.get(pd.name) match {
                    case Some(refHolder) => WrappedCreatorProperty(creator, refHolder)
                    case _ => creator
                  }
                  if (applyDefaultValues) {
                    pd match {
                      case PropertyDescriptor(_, Some(ConstructorParameter(_, _, Some(defaultValue))), _, _, _, _, _) => {
                        mappedCreator.withNullProvider(new NullValueProvider {
                          override def getNullValue(ctxt: DeserializationContext): AnyRef = defaultValue()

                          override def getNullAccessPattern: AccessPattern = AccessPattern.DYNAMIC
                        })
                      }
                      case _ => mappedCreator
                    }
                  } else {
                    mappedCreator
                  }
                }
                case _ => creator
              }
            }
          }
        }
        case Some(array) => array
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
        if (ScalaAnnotationIntrospectorModule.overrideMap.contains(beanDesc.getBeanClass)
          || descriptor.properties.exists(_.param.exists(_.defaultValue.isDefined))) {

          defaultInstantiator match {
            case std: StdValueInstantiator =>
              new ScalaValueInstantiator(ScalaAnnotationIntrospectorModule, std, config, descriptor)
            case other =>
              throw new IllegalArgumentException("Cannot customise a non StdValueInstantiator: " + other.getClass)
          }
        } else defaultInstantiator
      }.getOrElse(defaultInstantiator)

    } else defaultInstantiator
  }

  private def _descriptorFor(clz: Class[_]): Option[BeanDescriptor] = {
    if (clz.extendsScalaClass || clz.hasSignature) {
      val key = new ClassKey(clz)
      Option(ScalaAnnotationIntrospectorModule._descriptorCache.get(key)) match {
        case Some(result) => Some(result)
        case _ => {
          val introspector = BeanIntrospector(clz)
          ScalaAnnotationIntrospectorModule._descriptorCache.put(key, introspector)
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

  private[introspect] var _descriptorCache: LookupCache[ClassKey, BeanDescriptor] =
    new LRUMap[ClassKey, BeanDescriptor](16, 100)

  private[introspect] val overrideMap = MutableMap[Class[_], ClassOverrides]()

  /**
   * jackson-module-scala does not always properly handle deserialization of Options or Collections wrapping
   * Scala primitives (eg Int, Long, Boolean).
   * <p>
   * This function is experimental and may be removed or significantly reworked in a later release.
   * <p>
   * These issues can be worked around by adding Jackson annotations on the affected fields.
   * This function is designed to be used when it is not possible to apply Jackson annotations.
   *
   * @param clazz the (case) class
   * @param fieldName the field name in the (case) class
   * @param referencedType the referenced type of the field - for `Option[Long]` - the referenced type is `Long`
   * @see [[getRegisteredReferencedValueType]]
   * @see [[clearRegisteredReferencedTypes()]]
   * @see [[clearRegisteredReferencedTypes(Class[_])]]
   * @since 2.13.1
   */
  def registerReferencedValueType(clazz: Class[_], fieldName: String, referencedType: Class[_]): Unit = {
    val overrides = overrideMap.getOrElseUpdate(clazz, ClassOverrides()).overrides
    overrides.get(fieldName) match {
      case Some(holder) => overrides.put(fieldName, holder.copy(valueClass = Some(referencedType)))
      case _ => overrides.put(fieldName, ClassHolder(valueClass = Some(referencedType)))
    }
  }

  /**
   * jackson-module-scala does not always properly handle deserialization of Options or Collections wrapping
   * Scala primitives (eg Int, Long, Boolean).
   * <p>
   * This function is experimental and may be removed or significantly reworked in a later release.
   * <p>
   * These issues can be worked around by adding Jackson annotations on the affected fields.
   * This function is designed to be used when it is not possible to apply Jackson annotations.
   *
   * @param clazz the (case) class
   * @param fieldName the field name in the (case) class
   * @return the referenced type of the field - for `Option[Long]` - the referenced type is `Long`
   * @see [[registerReferencedValueType]]
   * @since 2.13.1
   */
  def getRegisteredReferencedValueType(clazz: Class[_], fieldName: String): Option[Class[_]] = {
    overrideMap.get(clazz).flatMap { overrides =>
      overrides.overrides.get(fieldName).flatMap(_.valueClass)
    }
  }

  /**
   * clears the state associated with reference types for the given class
   *
   * @param clazz the class for which to remove the registered reference types
   * @see [[registerReferencedValueType]]
   * @see [[clearRegisteredReferencedTypes()]]
   * @since 2.13.1
   */
  def clearRegisteredReferencedTypes(clazz: Class[_]): Unit = {
    overrideMap.remove(clazz)
  }

  /**
   * clears all the state associated with reference types
   *
   * @see [[registerReferencedValueType]]
   * @see [[clearRegisteredReferencedTypes(Class[_])]]
   * @since 2.13.1
   */
  def clearRegisteredReferencedTypes(): Unit = {
    overrideMap.clear()
  }

  def setDescriptorCache(cache: LookupCache[ClassKey, BeanDescriptor]): LookupCache[ClassKey, BeanDescriptor] = {
    val existingCache = _descriptorCache
    _descriptorCache = cache
    existingCache
  }
}

object ScalaAnnotationIntrospectorModule extends ScalaAnnotationIntrospectorModule

private case class WrappedCreatorProperty(creatorProperty: CreatorProperty, refHolder: ClassHolder)
  extends CreatorProperty(creatorProperty, creatorProperty.getFullName) {

  override def getType(): JavaType = {
    super.getType() match {
      case rt: ReferenceType if refHolder.valueClass.isDefined =>
        updateReferenceType(rt, refHolder.valueClass.get)
      case ct: CollectionLikeType if refHolder.valueClass.isDefined =>
        updateCollectionType(ct, refHolder.valueClass.get)
      case mt: MapLikeType if refHolder.valueClass.isDefined =>
        updateMapType(mt, refHolder.valueClass.get)
      case other => other
    }
  }

  private def updateReferenceType(rt: ReferenceType, newRefClass: Class[_]): ReferenceType = {
    rt.getContentType match {
      case innerRt: ReferenceType =>
        ReferenceType.upgradeFrom(rt, updateReferenceType(innerRt, newRefClass))
      case innerCt: CollectionLikeType =>
        ReferenceType.upgradeFrom(rt, updateCollectionType(innerCt, newRefClass))
      case innerMt: MapLikeType =>
        ReferenceType.upgradeFrom(rt, updateMapType(innerMt, newRefClass))
      case _ =>
        ReferenceType.upgradeFrom(rt, SimpleType.constructUnsafe(newRefClass))
    }
  }

  private def updateCollectionType(ct: CollectionLikeType, newRefClass: Class[_]): CollectionLikeType = {
    ct.getContentType match {
      case innerRt: ReferenceType =>
        CollectionLikeType.upgradeFrom(ct, updateReferenceType(innerRt, newRefClass))
      case innerCt: CollectionLikeType =>
        CollectionLikeType.upgradeFrom(ct, updateCollectionType(innerCt, newRefClass))
      case innerMt: MapLikeType =>
        CollectionLikeType.upgradeFrom(ct, updateMapType(innerMt, newRefClass))
      case _ =>
        CollectionLikeType.upgradeFrom(ct, SimpleType.constructUnsafe(newRefClass))
    }
  }

  private def updateMapType(mt: MapLikeType, newRefClass: Class[_]): MapLikeType = {
    mt.getContentType match {
      case innerRt: ReferenceType =>
        MapLikeType.upgradeFrom(mt, mt.getKeyType, updateReferenceType(innerRt, newRefClass))
      case innerCt: CollectionLikeType =>
        MapLikeType.upgradeFrom(mt, mt.getKeyType, updateCollectionType(innerCt, newRefClass))
      case innerMt: MapLikeType =>
        MapLikeType.upgradeFrom(mt, mt.getKeyType, updateMapType(innerMt, newRefClass))
      case _ =>
        MapLikeType.upgradeFrom(mt, mt.getKeyType, SimpleType.constructUnsafe(newRefClass))
    }
  }

}
