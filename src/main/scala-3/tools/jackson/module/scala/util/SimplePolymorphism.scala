package tools.jackson.module.scala.util

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, LookupCacheFactory, SimplePolymorphismSupport}

import java.lang.reflect.Modifier
import scala.util.Try

/**
 * Naming and lookup rules behind [[tools.jackson.module.scala.SimplePolymorphismSupport]].
 *
 * A Scala 3 `sealed` hierarchy leaves no trace of its implementations in the bytecode - the JVM
 * `PermittedSubclasses` attribute is not emitted, and unlike an `enum` the companion carries no
 * `Mirror.Sum`. So implementations are not enumerated. They do not need to be: writing a value only
 * needs its own class, and reading one only needs to turn a `@type` name back into a class. That is
 * done by deriving candidate class names from where the base type itself is declared, and keeping
 * only candidates that really are subtypes of it.
 */
private[scala] object SimplePolymorphism {

  /** Name of the JSON property that carries the derived type name. */
  val TypePropertyName = "@type"

  private val MarkerClass = classOf[SimplePolymorphismSupport]
  private val ModuleFieldName = "MODULE$"

  final case class Subtype(clazz: Class[_], singleton: Option[AnyRef])

  private final case class SubtypeKey(baseClass: Class[_], typeName: String)

  /**
   * True for a type in a marked hierarchy. A hierarchy that also carries `@JsonTypeInfo` is left to
   * the standard Jackson polymorphic handling rather than being tagged twice.
   */
  def isSupported(clazz: Class[_]): Boolean =
    MarkerClass.isAssignableFrom(clazz) && !hasJsonTypeInfo(clazz)

  // Jackson's own annotations are not @Inherited, so the hierarchy has to be walked
  private def hasJsonTypeInfo(clazz: Class[_]): Boolean = {
    clazz != null && MarkerClass.isAssignableFrom(clazz) && (
      clazz.getAnnotation(classOf[JsonTypeInfo]) != null ||
        hasJsonTypeInfo(clazz.getSuperclass) ||
        clazz.getInterfaces.exists(hasJsonTypeInfo))
  }

  /**
   * True for a type that is dispatched on rather than instantiated - the base of the hierarchy.
   */
  def isBaseType(clazz: Class[_]): Boolean =
    isSupported(clazz) && (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers))

  /**
   * The name written to `@type`: the implementation's simple name, with any enclosing types and the
   * trailing `$` of a Scala object removed. Never a fully qualified class name.
   */
  def typeNameFor(clazz: Class[_]): String = {
    val name = clazz.getName
    val afterPackage = name.substring(name.lastIndexOf('.') + 1)
    val afterModuleSuffix = if (afterPackage.endsWith("$")) afterPackage.dropRight(1) else afterPackage
    afterModuleSuffix.substring(afterModuleSuffix.lastIndexOf('$') + 1)
  }

  /**
   * Resolves a `@type` name to an implementation of `baseClass`, or `None` if the name does not
   * belong to that hierarchy.
   */
  def resolve(baseClass: Class[_], typeName: String): Option[Subtype] = {
    if (!isPlainName(typeName)) None
    else {
      val key = SubtypeKey(baseClass, typeName)
      val cache = _cache
      Option(cache.get(key)) match {
        case Some(subtype) => subtype
        case _ =>
          val subtype = findSubtype(baseClass, typeName)
          Option(cache.putIfAbsent(key, subtype)).getOrElse(subtype)
      }
    }
  }

  // guards against a `@type` value that tries to escape the hierarchy by naming a package, an
  // enclosing type or an array
  private def isPlainName(typeName: String): Boolean =
    typeName != null && typeName.nonEmpty && typeName.forall(c => c != '.' && c != '$' && c != '/' && c != '[' && c != ';')

  private def findSubtype(baseClass: Class[_], typeName: String): Option[Subtype] = {
    val loader = loaderFor(baseClass)
    candidateNames(baseClass.getName, typeName).view
      .flatMap(name => Try(Class.forName(name, false, loader)).toOption)
      .filter(candidate => baseClass.isAssignableFrom(candidate) && candidate != baseClass)
      .map(candidate => Subtype(candidate, moduleInstance(candidate)))
      .headOption
  }

  /**
   * Class names a `sealed` implementation could have been compiled to: nested inside the base type
   * or its companion, nested inside any object enclosing the base type, or a sibling in the same
   * package. A candidate that is not a subtype of the base is discarded by the caller, so the
   * companion of a case class and the static forwarder class of a case object are both ignored.
   */
  private def candidateNames(baseName: String, typeName: String): Seq[String] = {
    val packagePrefix = baseName.lastIndexOf('.') match {
      case -1 => ""
      case index => baseName.substring(0, index + 1)
    }
    val prefixes = Seq.newBuilder[String]
    prefixes += baseName + "$"
    var enclosing = baseName
    var separator = enclosing.lastIndexOf('$')
    while (separator > packagePrefix.length) {
      enclosing = enclosing.substring(0, separator)
      prefixes += enclosing + "$"
      separator = enclosing.lastIndexOf('$')
    }
    prefixes += packagePrefix
    // the object form is tried first - a case object's instances have the `$` class
    prefixes.result().distinct.flatMap(prefix => Seq(prefix + typeName + "$", prefix + typeName))
  }

  private def moduleInstance(clazz: Class[_]): Option[AnyRef] =
    Try(clazz.getField(ModuleFieldName).get(None.orNull)).toOption.map(_.asInstanceOf[AnyRef])

  private def loaderFor(clazz: Class[_]): ClassLoader =
    Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)

  private var _lookupCacheFactory: LookupCacheFactory = DefaultLookupCacheFactory
  private var _cacheSize: Int = 1000

  // bounded for the same reason as the Scala 3 enum case table cache - see Scala3EnumInfo
  @volatile private var _cache: LookupCache[SubtypeKey, Option[Subtype]] =
    _lookupCacheFactory.createLookupCache(16, _cacheSize)

  def setLookupCacheFactory(lookupCacheFactory: LookupCacheFactory): Unit = {
    _lookupCacheFactory = lookupCacheFactory
    recreateCache()
  }

  def setCacheSize(size: Int): Unit = {
    _cacheSize = size
    recreateCache()
  }

  def clearCache(): Unit = _cache.clear()

  private def recreateCache(): Unit = {
    _cache.clear()
    _cache = _lookupCacheFactory.createLookupCache(16, _cacheSize)
  }
}
