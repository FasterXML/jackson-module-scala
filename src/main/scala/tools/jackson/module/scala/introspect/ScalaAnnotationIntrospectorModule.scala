package tools.jackson.module.scala.introspect

import com.fasterxml.jackson.annotation.JsonCreator
import tools.jackson.core.Version
import tools.jackson.databind.JacksonModule.SetupContext
import tools.jackson.databind.`type`.{CollectionLikeType, MapLikeType, ReferenceType, SimpleType}
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.deser._
import tools.jackson.databind.deser.std.StdValueInstantiator
import tools.jackson.databind.introspect._
import tools.jackson.databind.util.{AccessPattern, LookupCache, SimpleLookupCache}
import tools.jackson.databind.{BeanDescription, DeserializationConfig, DeserializationContext, JavaType, MapperFeature, PropertyName}
import tools.jackson.module.scala.JacksonModule.InitializerBuilder
import tools.jackson.module.scala.{DefaultLookupCacheFactory, JacksonModule, LookupCacheFactory, ScalaModule}
import tools.jackson.module.scala.util.Implicits._

import java.lang.annotation.Annotation
import scala.collection.mutable.{Map => MutableMap}

class ScalaAnnotationIntrospectorInstance(scalaAnnotationIntrospectorModule: ScalaAnnotationIntrospectorModule,
                                          config: ScalaModule.Config) extends NopAnnotationIntrospector with ValueInstantiators {
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

  override def findImplicitPropertyName(mapperConfig: MapperConfig[_], member: AnnotatedMember): String = {
    member match {
      case af: AnnotatedField => fieldName(af).orNull
      case am: AnnotatedMethod => methodName(am).orNull
      case ap: AnnotatedParameter => paramName(ap).orNull
      case _ => None.orNull
    }
  }

  override def findNameForDeserialization(mapperConfig: MapperConfig[_], ann: Annotated): PropertyName = {
    Option(mapperConfig.getPropertyNamingStrategy) match {
      case Some(_) => None.orNull
      case _ => {
        val modifiedName = ann match {
          case af: AnnotatedField if af.getName.contains("$") => fieldName(af)
          case am: AnnotatedMethod if am.getName.contains("$") => methodName(am)
          case ap: AnnotatedParameter if ap.getName.contains("$") => paramName(ap)
          case _ => None
        }
        modifiedName.map(new PropertyName(_)).orNull
      }
    }
  }

  override def hasIgnoreMarker(mapperConfig: MapperConfig[_], m: AnnotatedMember): Boolean = {
    val name = m.getName
    //special cases to prevent shadow fields associated with lazy vals being serialized
    name == "0bitmap$1" || name.endsWith("$lzy1") || name.contains("$default$") || super.hasIgnoreMarker(mapperConfig, m)
  }

  private def hasCreatorAnnotation(a: Annotated): Boolean = {
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

  private def findCreatorBinding(a: Annotated): JsonCreator.Mode = {
    Option(_findAnnotation(a, classOf[JsonCreator])) match {
      case Some(ann) => ann.mode()
      case _ => {
        if (isScala(a) && hasCreatorAnnotation(a)) {
          JsonCreator.Mode.PROPERTIES
        } else None.orNull
      }
    }
  }

  override def modifyValueInstantiator(deserializationConfig: DeserializationConfig, beanDesc: BeanDescription,
                                       defaultInstantiator: ValueInstantiator): ValueInstantiator = {
    if (scalaAnnotationIntrospectorModule.isMaybeScalaBeanType(beanDesc.getBeanClass)) {
      _descriptorFor(beanDesc.getBeanClass).map { descriptor =>
        if (scalaAnnotationIntrospectorModule.overrideMap.contains(beanDesc.getBeanClass.getName) || descriptor.properties.exists(_.param.exists(_.defaultValue.isDefined))) {
          defaultInstantiator match {
            case std: StdValueInstantiator =>
              new ScalaValueInstantiator(config, std, deserializationConfig, descriptor)
            case other =>
              throw new IllegalArgumentException("Cannot customise a non StdValueInstantiator: " + other.getClass)
          }
        } else defaultInstantiator
      }.getOrElse(defaultInstantiator)

    } else defaultInstantiator
  }

  override def findValueInstantiator(deserializationConfig: DeserializationConfig, beanDesc: BeanDescription): ValueInstantiator = {
    None.orNull
  }

  override def version(): Version = JacksonModule.version

  private def _descriptorFor(clz: Class[_]): Option[BeanDescriptor] = {
    val key = clz.getName
    val isScala = {
      Option(ScalaAnnotationIntrospectorModule._scalaTypeCache.get(key)) match {
        case Some(result) => result
        case _ => {
          val result = clz.extendsScalaClass(config.shouldSupportScala3Classes()) || clz.hasSignature
          ScalaAnnotationIntrospectorModule._scalaTypeCache.put(key, result)
          result
        }
      }
    }
    if (isScala) {
      Option(scalaAnnotationIntrospectorModule._descriptorCache.get(key)) match {
        case Some(result) => Some(result)
        case _ => {
          val introspector = BeanIntrospector(clz)
          scalaAnnotationIntrospectorModule._descriptorCache.put(key, introspector)
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

  private def isScala(a: Annotated): Boolean = {
    a match {
      case ac: AnnotatedClass => scalaAnnotationIntrospectorModule.isMaybeScalaBeanType(ac.getAnnotated)
      case am: AnnotatedMember => scalaAnnotationIntrospectorModule.isMaybeScalaBeanType(am.getDeclaringClass)
    }
  }

  private class ScalaValueInstantiator(config: ScalaModule.Config, delegate: StdValueInstantiator,
                                       deserializationConfig: DeserializationConfig, descriptor: BeanDescriptor)
    extends StdValueInstantiator(delegate) {

    private val overriddenConstructorArguments: Array[SettableBeanProperty] = {
      val overrides = scalaAnnotationIntrospectorModule.overrideMap.get(descriptor.beanType.getName).map(_.overrides.toMap).getOrElse(Map.empty)
      val applyDefaultValues = deserializationConfig.isEnabled(MapperFeature.APPLY_DEFAULT_VALUES) &&
        config.shouldApplyDefaultValuesWhenDeserializing()
      val args = delegate.getFromObjectArguments(deserializationConfig)
      Option(args) match {
        case Some(array) if (applyDefaultValues || overrides.nonEmpty) => {
          array.map {
            case creator: CreatorProperty => {
              // Locate the constructor param that matches it
              descriptor.properties.find(_.param.exists(_.index == creator.getCreatorIndex)) match {
                case Some(pd) => {
                  if (applyDefaultValues) {
                    pd match {
                      case PropertyDescriptor(_, Some(ConstructorParameter(_, _, Some(defaultValue))), _, _, _, _, _) => {
                        val updatedCreator = creator.withNullProvider(new NullValueProvider {
                          override def getNullValue(ctxt: DeserializationContext): AnyRef = defaultValue()
                          override def getNullAccessPattern: AccessPattern = AccessPattern.DYNAMIC
                        })
                        updatedCreator match {
                          case cp: CreatorProperty => applyOverrides(cp, pd.name, overrides)
                          case cp => cp
                        }
                      }
                      case _ => applyOverrides(creator, pd.name, overrides)
                    }
                  } else {
                    applyOverrides(creator, pd.name, overrides)
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

    private def applyOverrides(creator: CreatorProperty, propertyName: String,
                               overrides: Map[String, ClassHolder]): CreatorProperty = {
      overrides.get(propertyName) match {
        case Some(refHolder) => WrappedCreatorProperty(creator, refHolder)
        case _ => creator
      }
    }
  }
}

trait ScalaAnnotationIntrospectorModule extends JacksonModule {

  override def getInitializers(config: ScalaModule.Config): Seq[SetupContext => Unit] = {
    val builder = new InitializerBuilder()
    val sai = new ScalaAnnotationIntrospectorInstance(this, config)
    builder += { _.appendAnnotationIntrospector(new JavaAnnotationIntrospectorInstance(config)) }
    builder += { _.appendAnnotationIntrospector(sai) }
    builder += { _.addValueInstantiators(sai) }
    builder.build()
  }

  private var _lookupCacheFactory: LookupCacheFactory = DefaultLookupCacheFactory
  private var _descriptorCacheSize: Int = 100
  private var _scalaTypeCacheSize: Int = 1000

  private[introspect] val overrideMap = MutableMap[String, ClassOverrides]()

  private[introspect] var _descriptorCache: LookupCache[String, BeanDescriptor] =
    _lookupCacheFactory.createLookupCache(16, _descriptorCacheSize)

  private[introspect] var _scalaTypeCache: LookupCache[String, Boolean] =
    _lookupCacheFactory.createLookupCache(16, _scalaTypeCacheSize)

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
   * @since 2.13.0
   */
  def registerReferencedValueType(clazz: Class[_], fieldName: String, referencedType: Class[_]): Unit = {
    val overrides = overrideMap.getOrElseUpdate(clazz.getName, ClassOverrides()).overrides
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
    overrideMap.get(clazz.getName).flatMap { overrides =>
      overrides.overrides.get(fieldName).flatMap(_.valueClass)
    }
  }

  /**
   * clears the state associated with reference types for the given class
   *
   * @param clazz the class for which to remove the registered reference types
   * @see [[registerReferencedValueType]]
   * @see [[clearRegisteredReferencedTypes()]]
   * @since 2.13.0
   */
  def clearRegisteredReferencedTypes(clazz: Class[_]): Unit = {
    overrideMap.remove(clazz.getName)
  }

  /**
   * Clears all the state associated with reference types
   *
   * @see [[registerReferencedValueType]]
   * @see [[clearRegisteredReferencedTypes(Class[_])]]
   * @since 2.13.0
   */
  def clearRegisteredReferencedTypes(): Unit = {
    overrideMap.clear()
  }

  /**
   * Replaces the [[LookupCacheFactory]]. The default factory uses [[tools.jackson.databind.util.SimpleLookupCache]].
   * <p>
   * Note that this clears the existing cache entries. It is best to set this up before you start using
   * the Jackson Scala Module for serializing/deserializing.
   * </p>
   *
   * @param lookupCacheFactory new factory
   * @see [[setDescriptorCacheSize]]
   * @see [[setScalaTypeCacheSize]]
   * @since 2.14.3
   */
  def setLookupCacheFactory(lookupCacheFactory: LookupCacheFactory): Unit = {
    _lookupCacheFactory = lookupCacheFactory
    recreateDescriptorCache()
    recreateScalaTypeCache()
  }

  /**
   * Resize the <code>descriptorCache</code>. The default size is 100.
   * <p>
   * Note that this clears the existing cache entries. It is best to set this up before you start using
   * the Jackson Scala Module for serializing/deserializing.
   * </p>
   *
   * @param size new size for the cache
   * @see [[setScalaTypeCacheSize]]
   * @see [[setLookupCacheFactory]]
   * @since 2.14.3
   */
  def setDescriptorCacheSize(size: Int): Unit = {
    _descriptorCacheSize = size
    recreateDescriptorCache()
  }

  /**
   * Resize the <code>scalaTypeCache</code>. The default size is 1000.
   * <p>
   * Note that this clears the existing cache entries. It is best to set this up before you start using
   * the Jackson Scala Module for serializing/deserializing.
   * </p>
   *
   * @param size new size for the cache
   * @see [[setDescriptorCacheSize]]
   * @see [[setLookupCacheFactory]]
   * @since 2.14.3
   */
  def setScalaTypeCacheSize(size: Int): Unit = {
    _scalaTypeCacheSize = size
    recreateScalaTypeCache()
  }

  private def isScalaPackage(pkg: Option[Package]): Boolean =
    pkg.exists(_.getName.startsWith("scala."))

  private[introspect] def isMaybeScalaBeanType(cls: Class[_]): Boolean =
    (cls.extendsScalaClass(config.shouldSupportScala3Classes()) || cls.hasSignature) &&
      !isScalaPackage(Option(cls.getPackage))

  private def recreateDescriptorCache(): Unit = {
    _descriptorCache.clear()
    _descriptorCache = _lookupCacheFactory.createLookupCache(16, _descriptorCacheSize)
  }

  private def recreateScalaTypeCache(): Unit = {
    _scalaTypeCache.clear()
    _scalaTypeCache = _lookupCacheFactory.createLookupCache(16, _scalaTypeCacheSize)
  }

}

object ScalaAnnotationIntrospectorModule extends ScalaAnnotationIntrospectorModule {
  /**
   * @return a standalone instance of ScalaAnnotationIntrospectorModule with none of the saved state from the
   *         ScalaAnnotationIntrospectorModule object instance
   */
  def newStandaloneInstance(): ScalaAnnotationIntrospectorModule
    = new ScalaAnnotationIntrospectorModule {}
}

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
