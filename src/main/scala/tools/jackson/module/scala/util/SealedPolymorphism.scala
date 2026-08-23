package tools.jackson.module.scala.util

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, LookupCacheFactory, SealedPolymorphismSupport}

import java.lang.reflect.Modifier
import scala.util.Try

/**
 * The naming rules behind [[tools.jackson.module.scala.SealedPolymorphismSupport]] that depend on
 * nothing but the classes themselves. A name is derived identically on every Scala version, so a
 * value written by one can be read by another.
 *
 * Everything that holds state - the caches, and the lookup that fills them - lives on the
 * [[SealedPolymorphism]] instance rather than here, so that a module built with
 * [[tools.jackson.module.scala.ScalaModule.Builder]] keeps its own.
 */
private[scala] object SealedPolymorphism {

  /** Name of the JSON property that carries the derived type name. */
  val TypePropertyName = "@type"

  private[scala] val MarkerClass = classOf[SealedPolymorphismSupport]
  private val ModuleFieldName = "MODULE$"

  final case class Subtype(clazz: Class[_], singleton: Option[AnyRef])

  private[scala] final case class SubtypeKey(baseClass: Class[_], typeName: String)

  private[scala] def conflictMessage(clazz: Class[_]): String =
    s"${clazz.getName} carries @JsonTypeInfo but belongs to the ${classOf[SealedPolymorphismSupport].getSimpleName} " +
      s"hierarchy rooted at ${rootOf(clazz).getName}. Move the annotation to the root to use Jackson's polymorphic " +
      s"handling for the whole hierarchy, or remove it to use $TypePropertyName."

  private[scala] def nameTakenMessage(clazz: Class[_], typeName: String, taken: Class[_]): String =
    s"${clazz.getName} is written as $TypePropertyName '$typeName', but that name already belongs to " +
      s"${taken.getName}. Rename one of them so that the two derive different names."

  private[scala] def notFoundMessage(clazz: Class[_], typeName: String, root: Class[_]): String =
    s"${clazz.getName} is written as $TypePropertyName '$typeName', but no subtype of ${root.getName} is " +
      s"declared under that name alongside it. An implementation has to be declared in the same file as " +
      s"${root.getName} - which is what `sealed` guarantees - so that it can be found again when reading."

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

  /** True for a type that can hold a value of its own, so can carry a name of its own. */
  def isConcrete(clazz: Class[_]): Boolean =
    !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)

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

  // guards against a `@type` value that tries to escape the hierarchy by naming a package or an
  // array. `$` is allowed, since a name may carry the object that encloses the implementation, but
  // a candidate is still only reachable if it turns out to be a subtype of the base
  private[scala] def isPlainName(typeName: String): Boolean =
    typeName != null && typeName.nonEmpty && typeName.forall(c => c != '.' && c != '/' && c != '[' && c != ';')

  /**
   * Where an implementation of the hierarchy could have been declared, longest prefix first: nested
   * inside the root or its companion, nested inside any object enclosing the root, or alongside the
   * root in its package.
   */
  private[scala] def prefixesFor(rootName: String): Seq[String] = {
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

  private[scala] def moduleInstance(clazz: Class[_]): Option[AnyRef] =
    Try(clazz.getField(ModuleFieldName).get(None.orNull)).toOption.map(_.asInstanceOf[AnyRef])

  private[scala] def loaderFor(clazz: Class[_]): ClassLoader =
    Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)
}

/**
 * The state behind [[tools.jackson.module.scala.SealedPolymorphismSupport]]: the cache of resolved
 * `@type` names, and the version specific [[SubtypeLookup]] that fills it.
 *
 * One of these belongs to each module instance, so a `ScalaModule` built through its builder keeps
 * its own caches and its own cache settings, while the `DefaultScalaModule` object shares the one
 * that comes with the [[tools.jackson.module.scala.SealedPolymorphismModule]] object.
 */
private[scala] class SealedPolymorphism {

  import SealedPolymorphism._

  // `this` is handed over, so it is only built on first use, once this instance is fully constructed
  private lazy val lookup: SubtypeLookup = new SubtypeLookup(this)

  /**
   * True for a type in a marked hierarchy that this module should handle.
   *
   * Two hierarchies are handed back: a Scala 3 `enum`, which the enum support already tags from an
   * exact case table, and one whose root carries `@JsonTypeInfo`, which is left to the standard
   * Jackson polymorphic handling rather than being tagged twice.
   *
   * This is the first thing every path asks, and the marker check comes first, so it is also where
   * a lookup that cannot run at all is reported - only ever for a type that actually uses the
   * marker, never for the rest of an application's classes.
   */
  def isSupported(clazz: Class[_]): Boolean = {
    MarkerClass.isAssignableFrom(clazz) && {
      lookup.checkAvailable(clazz)
      !lookup.isScalaEnum(clazz) && rootOf(clazz).getAnnotation(classOf[JsonTypeInfo]) == null
    }
  }

  /**
   * `@JsonTypeInfo` on an implementation rather than on the root cannot be honoured alongside
   * `@type`: Jackson treats the annotated class as a polymorphic base in its own right, so reading
   * it demands that annotation's type id, which nothing in a marked hierarchy ever writes. The
   * combination is reported rather than left to produce JSON that cannot be read back.
   */
  def conflictingJsonTypeInfo(clazz: Class[_]): Boolean =
    isSupported(clazz) && clazz.getAnnotation(classOf[JsonTypeInfo]) != null

  /**
   * True for a type that can only be dispatched on, never instantiated - a trait or abstract class.
   */
  def isBaseType(clazz: Class[_]): Boolean = isSupported(clazz) && !isConcrete(clazz)

  /**
   * True for a concrete type that other implementations may extend - `sealed class Node` is both a
   * value in its own right and a base its subclasses are read through, and so is any concrete class
   * part way down such a hierarchy. A property declared at one of these has to dispatch rather than
   * read straight through to the bean, or a subclass would be silently read back as the type it was
   * declared as.
   */
  def needsSubtypeDispatch(clazz: Class[_]): Boolean =
    isSupported(clazz) && isConcrete(clazz) && lookup.mayHaveSubtypes(clazz)

  /**
   * Checks that an implementation can be found again from the name it is written under, and
   * describes the problem if it cannot.
   *
   * Serializing needs only the value's own class, so without this check an implementation that
   * resolution cannot reach would be written happily and fail only when something tried to read it
   * back - possibly in another process.
   */
  def unreachableReason(clazz: Class[_]): Option[String] = lookup.unreachableReason(clazz)

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
          val subtype = lookup.findSubtype(baseClass, typeName)
          Option(cache.putIfAbsent(key, subtype)).getOrElse(subtype)
      }
    }
  }

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

  def clearCache(): Unit = {
    _cache.clear()
    lookup.clearCache()
  }

  private def recreateCache(): Unit = {
    clearCache()
    _cache = _lookupCacheFactory.createLookupCache(16, _cacheSize)
  }
}
