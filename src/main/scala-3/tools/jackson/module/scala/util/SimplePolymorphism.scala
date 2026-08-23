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
  private val EnumClass = classOf[scala.reflect.Enum]
  private val ModuleFieldName = "MODULE$"

  final case class Subtype(clazz: Class[_], singleton: Option[AnyRef])

  private final case class SubtypeKey(baseClass: Class[_], typeName: String)

  /**
   * True for a type in a marked hierarchy that this module should handle.
   *
   * Two hierarchies are handed back: a Scala 3 `enum`, which the enum support already tags from an
   * exact case table, and one whose root carries `@JsonTypeInfo`, which is left to the standard
   * Jackson polymorphic handling rather than being tagged twice.
   */
  def isSupported(clazz: Class[_]): Boolean =
    MarkerClass.isAssignableFrom(clazz) &&
      !EnumClass.isAssignableFrom(clazz) &&
      rootOf(clazz).getAnnotation(classOf[JsonTypeInfo]) == null

  /**
   * `@JsonTypeInfo` on an implementation rather than on the root cannot be honoured alongside
   * `@type`: Jackson treats the annotated class as a polymorphic base in its own right, so reading
   * it demands that annotation's type id, which nothing in a marked hierarchy ever writes. The
   * combination is reported rather than left to produce JSON that cannot be read back.
   */
  def conflictingJsonTypeInfo(clazz: Class[_]): Boolean =
    isSupported(clazz) && clazz.getAnnotation(classOf[JsonTypeInfo]) != null

  def conflictMessage(clazz: Class[_]): String =
    s"${clazz.getName} carries @JsonTypeInfo but belongs to the ${classOf[SimplePolymorphismSupport].getSimpleName} " +
      s"hierarchy rooted at ${rootOf(clazz).getName}. Move the annotation to the root to use Jackson's polymorphic " +
      s"handling for the whole hierarchy, or remove it to use $TypePropertyName."

  /**
   * The top of the marked hierarchy `clazz` belongs to. The opt-out is read from the root rather
   * than from `clazz` itself so that both halves of the module agree: an annotation on one
   * implementation governs that implementation's own subtypes, and must not silently switch off
   * tagging for it while the base is still dispatching on `@type`.
   */
  def rootOf(clazz: Class[_]): Class[_] = {
    var root = clazz
    var parent = markedParentOf(root)
    while (parent.isDefined) {
      root = parent.get
      parent = markedParentOf(root)
    }
    root
  }

  private def markedParentOf(clazz: Class[_]): Option[Class[_]] =
    (Option(clazz.getSuperclass).toSeq ++ clazz.getInterfaces)
      .find(parent => parent != MarkerClass && MarkerClass.isAssignableFrom(parent))

  /**
   * True for a type that is dispatched on rather than instantiated - the base of the hierarchy.
   */
  def isBaseType(clazz: Class[_]): Boolean =
    isSupported(clazz) && (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers))

  /**
   * The name written to `@type`: the implementation's class name with the longest prefix shared
   * with the hierarchy root removed, and the trailing `$` of a Scala object dropped. Never a fully
   * qualified class name.
   *
   * An implementation declared beside the root, or inside the root's companion, keeps its simple
   * name. One declared inside some other object keeps that object in its name - `Left$Same` rather
   * than `Same` - so that two objects can each hold an implementation of the same name.
   *
   * The prefixes are the same ones [[resolve]] searches, so a name always names exactly the class
   * it came from.
   */
  def typeNameFor(clazz: Class[_]): String = {
    val name = clazz.getName
    val withoutModuleSuffix = if (name.endsWith("$")) name.dropRight(1) else name
    // prefixes run longest to shortest, so the first match is the most specific
    prefixesFor(rootOf(clazz).getName).find(withoutModuleSuffix.startsWith) match {
      case Some(prefix) => withoutModuleSuffix.substring(prefix.length)
      case None => withoutModuleSuffix.substring(withoutModuleSuffix.lastIndexOf('.') + 1)
    }
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

  // guards against a `@type` value that tries to escape the hierarchy by naming a package or an
  // array. `$` is allowed, since a name may carry the object that encloses the implementation, but
  // a candidate is still only reachable if it turns out to be a subtype of the base
  private def isPlainName(typeName: String): Boolean =
    typeName != null && typeName.nonEmpty && typeName.forall(c => c != '.' && c != '/' && c != '[' && c != ';')

  private def findSubtype(baseClass: Class[_], typeName: String): Option[Subtype] = {
    val loader = loaderFor(baseClass)
    // anchored on the root, so a property declared as an intermediate type still resolves the names
    // that were written for the hierarchy as a whole
    candidateNames(rootOf(baseClass).getName, typeName).view
      .flatMap(name => Try(Class.forName(name, false, loader)).toOption)
      .filter(candidate => baseClass.isAssignableFrom(candidate) && candidate != baseClass)
      .map(candidate => Subtype(candidate, moduleInstance(candidate)))
      .headOption
  }

  /**
   * Class names a `sealed` implementation could have been compiled to. A candidate that is not a
   * subtype of the base is discarded by the caller, so the companion of a case class and the static
   * forwarder class of a case object are both ignored.
   */
  private def candidateNames(rootName: String, typeName: String): Seq[String] =
    // the object form is tried first - a case object's instances have the `$` class
    prefixesFor(rootName).flatMap(prefix => Seq(prefix + typeName + "$", prefix + typeName))

  /**
   * Where an implementation of the hierarchy could have been declared, longest prefix first: nested
   * inside the root or its companion, nested inside any object enclosing the root, or alongside the
   * root in its package.
   */
  private def prefixesFor(rootName: String): Seq[String] = {
    val packagePrefix = rootName.lastIndexOf('.') match {
      case -1 => ""
      case index => rootName.substring(0, index + 1)
    }
    val prefixes = Seq.newBuilder[String]
    prefixes += rootName + "$"
    var enclosing = rootName
    var separator = enclosing.lastIndexOf('$')
    while (separator > packagePrefix.length) {
      enclosing = enclosing.substring(0, separator)
      prefixes += enclosing + "$"
      separator = enclosing.lastIndexOf('$')
    }
    prefixes += packagePrefix
    prefixes.result().distinct
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
