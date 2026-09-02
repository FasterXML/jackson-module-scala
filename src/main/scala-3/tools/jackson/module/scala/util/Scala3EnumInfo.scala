package tools.jackson.module.scala.util

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, LookupCacheFactory}

import java.lang.reflect.Modifier
import scala.deriving.Mirror
import scala.util.Try

/**
 * Runtime view of the cases of a Scala 3 `enum`.
 *
 * Apache Fory builds the same table at compile time with a macro (`ScalaJsonCodecMacros` produces
 * the case classes, case names and singleton instances that `DerivedScalaJsonCodec` dispatches on).
 * jackson-module-scala only ever sees a `Class[_]`, so the table is recovered reflectively instead:
 * the compiler emits one public static field per case on the enum companion, holding either the
 * singleton instance (simple case) or the companion object of the generated case class
 * (parameterized case).
 *
 * Deriving a table depends on nothing but the enum class, so it lives here. Remembering one is
 * state, and lives on the [[Scala3EnumInfo]] instance rather than here, so that a module built with
 * [[tools.jackson.module.scala.ScalaModule.Builder]] keeps its own.
 */
private[scala] object Scala3EnumInfo {

  /**
   * Name of the JSON property used to tag a parameterized enum case. `@type` rather than `type` so
   * that it cannot clash with a field of the case itself - it is also what Jackson uses by default
   * for `JsonTypeInfo.Id.NAME`.
   */
  val TypePropertyName = "@type"

  private val ModuleFieldName = "MODULE$"
  private val EnumClass = classOf[scala.reflect.Enum]

  final case class EnumCase(name: String, clazz: Class[_], singleton: Option[AnyRef])

  final class Info(val rootClass: Class[_], val cases: Seq[EnumCase]) {
    private val casesByName: Map[String, EnumCase] = cases.map(c => c.name -> c).toMap
    // singleton cases of one enum all share a single anonymous class, so only parameterized
    // cases can be looked up by class
    private val casesByClass: Map[Class[_], EnumCase] =
      cases.collect { case c if c.singleton.isEmpty => c.clazz -> c }.toMap

    val hasParameterizedCases: Boolean = casesByClass.nonEmpty

    /**
     * True if this enum should be handled as a tagged sum. Enums that already carry
     * `@JsonTypeInfo` are left to the standard Jackson polymorphic handling.
     */
    val isTaggedSum: Boolean =
      hasParameterizedCases && rootClass.getAnnotation(classOf[JsonTypeInfo]) == null

    def caseForName(name: String): Option[EnumCase] = casesByName.get(name)
    def parameterizedCaseFor(clazz: Class[_]): Option[EnumCase] = casesByClass.get(clazz)
  }

  private[scala] def isEnumClass(clazz: Class[_]): Boolean =
    clazz != null && EnumClass.isAssignableFrom(clazz)

  private[scala] def findInfo(clazz: Class[_]): Option[Info] = {
    var current: Class[_] = clazz
    var result: Option[Info] = None
    while (result.isEmpty && current != null && EnumClass.isAssignableFrom(current)) {
      result = buildInfo(current)
      current = current.getSuperclass
    }
    result
  }

  private def buildInfo(rootClass: Class[_]): Option[Info] = {
    companionOf(rootClass).filter(_.isInstanceOf[Mirror.Sum]).flatMap { companion =>
      val cases = companion.getClass.getDeclaredFields.toSeq.filter { field =>
        val modifiers = field.getModifiers
        Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) &&
          field.getName != ModuleFieldName && !field.getName.startsWith("$")
      }.flatMap { field =>
        val name = field.getName
        if (rootClass.isAssignableFrom(field.getType)) {
          // simple case - the field holds the singleton instance
          Option(field.get(None.orNull)).map { value =>
            EnumCase(name, value.getClass, Some(value.asInstanceOf[AnyRef]))
          }
        } else if (field.getType.getName.endsWith("$")) {
          // parameterized case - the field holds the companion of the generated case class
          val caseClassName = field.getType.getName.dropRight(1)
          Try(Class.forName(caseClassName, false, loaderFor(rootClass))).toOption
            .filter(rootClass.isAssignableFrom)
            .map(caseClass => EnumCase(name, caseClass, None))
        } else None
      }
      if (cases.isEmpty) None else Some(new Info(rootClass, cases))
    }
  }

  private def companionOf(clazz: Class[_]): Option[AnyRef] = {
    Try {
      val companionClass = Class.forName(clazz.getName + "$", false, loaderFor(clazz))
      companionClass.getField(ModuleFieldName).get(None.orNull)
    }.toOption
  }

  private def loaderFor(clazz: Class[_]): ClassLoader =
    Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)
}

/**
 * Remembers the case table of each Scala 3 enum met.
 *
 * One of these belongs to each module instance, so a `ScalaModule` built through its builder keeps
 * its own cache and its own cache settings, while the `DefaultScalaModule` object shares the one
 * that comes with the [[tools.jackson.module.scala.EnumModule]] object.
 */
private[scala] class Scala3EnumInfo {

  import Scala3EnumInfo._

  private var _lookupCacheFactory: LookupCacheFactory = DefaultLookupCacheFactory
  private var _cacheSize: Int = 1000

  // Bounded so that classes loaded by a child classloader (hot redeploy, OSGi, script engines) are
  // not retained indefinitely. Keyed by Class rather than by class name because the cached Info
  // holds Class instances - two same-named enums from different classloaders must not share an
  // entry. The cache is a pure memo - an evicted entry is rebuilt on the next lookup.
  @volatile private var _cache: LookupCache[Class[_], Option[Info]] =
    _lookupCacheFactory.createLookupCache(16, _cacheSize)

  /**
   * Returns the case table of the Scala 3 enum that `clazz` belongs to - `clazz` may be the enum
   * itself, one of its parameterized case classes, or the anonymous class used for its simple cases.
   */
  def infoFor(clazz: Class[_]): Option[Info] = {
    if (!isEnumClass(clazz)) None
    else {
      val cache = _cache
      Option(cache.get(clazz)) match {
        case Some(info) => info
        case _ =>
          val info = findInfo(clazz)
          Option(cache.putIfAbsent(clazz, info)).getOrElse(info)
      }
    }
  }

  /** Enums that mix parameterized and simple cases, which are tagged rather than named. */
  def taggedSumInfo(clazz: Class[_]): Option[Info] = infoFor(clazz).filter(_.isTaggedSum)

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
