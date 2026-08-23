package tools.jackson.module.scala.util

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, SealedPolymorphismSupport}
import tools.jackson.module.scala.util.SealedPolymorphism.Subtype

import scala.util.Try

/**
 * Finds the implementations of a marked hierarchy on Scala 2.
 *
 * Scala 2 keeps its symbol table in the class file, so scala-reflect can enumerate a sealed
 * hierarchy exactly and can confirm that the base really is `sealed` - neither of which Scala 3
 * allows. The names are derived by the shared rule, so the JSON is identical either way; only the
 * way an implementation is found again differs.
 *
 * scala-reflect is an optional dependency. It is needed only by applications that use the marker
 * trait, so its absence is reported the first time a marked type is met rather than at startup, and
 * never for an application that does not use the feature.
 */
private[scala] object SubtypeLookup {

  private val ScalaReflectClassName = "scala.reflect.runtime.package$"

  private lazy val scalaReflectAvailable: Boolean =
    Try(Class.forName(ScalaReflectClassName, false, SealedPolymorphism.loaderFor(MarkerLoaderAnchor))).isSuccess

  private val MarkerLoaderAnchor = classOf[SealedPolymorphismSupport]

  /** Scala 2 has no `scala.reflect.Enum` - `Enumeration` is handled by a different module. */
  def isScalaEnum(clazz: Class[_]): Boolean = false

  def checkAvailable(clazz: Class[_]): Unit = {
    if (!scalaReflectAvailable) {
      throw new IllegalStateException(
        s"${clazz.getName} uses ${classOf[SealedPolymorphismSupport].getSimpleName}, which needs the scala-reflect " +
          "jar on the runtime classpath to find the implementations of a sealed hierarchy. Add " +
          "\"org.scala-lang\" % \"scala-reflect\" % scalaVersion.value as a dependency, or drop the marker trait and " +
          "use @JsonTypeInfo with @JsonSubTypes instead.")
    }
  }

  def findSubtype(baseClass: Class[_], typeName: String): Option[Subtype] = {
    tableFor(SealedPolymorphism.rootOf(baseClass)).byName.get(typeName)
      .filter(subtype => baseClass.isAssignableFrom(subtype.clazz) && subtype.clazz != baseClass)
  }

  /**
   * Checks the hierarchy `clazz` belongs to, rather than just `clazz`: because the implementations
   * are enumerated, a base that is not `sealed` and a name claimed by two implementations can both
   * be reported the first time the hierarchy is used, instead of one implementation at a time.
   */
  def unreachableReason(clazz: Class[_]): Option[String] = {
    val root = SealedPolymorphism.rootOf(clazz)
    val table = tableFor(root)
    val typeName = SealedPolymorphism.typeNameFor(clazz)
    if (!table.sealedRoot) {
      Some(s"${root.getName} is not sealed. ${classOf[SealedPolymorphismSupport].getSimpleName} needs a sealed " +
        "hierarchy, so that every implementation can be found again when reading.")
    } else table.duplicates.get(typeName) match {
      case Some(clashing) =>
        Some(SealedPolymorphism.nameTakenMessage(clazz, typeName, clashing.find(_ != clazz).getOrElse(clazz)))
      case None => table.byName.get(typeName) match {
        case Some(subtype) if subtype.clazz == clazz => None
        case Some(subtype) => Some(SealedPolymorphism.nameTakenMessage(clazz, typeName, subtype.clazz))
        case None => Some(SealedPolymorphism.notFoundMessage(clazz, typeName, root))
      }
    }
  }

  private final case class Table(sealedRoot: Boolean, byName: Map[String, Subtype],
                                 duplicates: Map[String, Seq[Class[_]]])

  private def tableFor(root: Class[_]): Table = {
    val cache = _cache
    Option(cache.get(root)) match {
      case Some(table) => table
      case _ =>
        val table = buildTable(root)
        Option(cache.putIfAbsent(root, table)).getOrElse(table)
    }
  }

  private def buildTable(root: Class[_]): Table = {
    import scala.reflect.runtime.{universe => ru}
    val mirror = ru.runtimeMirror(SealedPolymorphism.loaderFor(root))
    val rootSymbol = mirror.classSymbol(root)
    if (!rootSymbol.isSealed) Table(sealedRoot = false, Map.empty, Map.empty)
    else {
      def concreteBelow(symbol: ru.ClassSymbol): Seq[ru.ClassSymbol] = {
        val direct = symbol.knownDirectSubclasses.toSeq.collect { case child if child.isClass => child.asClass }
        direct.flatMap { child =>
          val below = concreteBelow(child)
          // an intermediate sealed trait or abstract class is descended into, not named itself
          if (child.isTrait || child.isAbstract) below else child +: below
        }
      }
      // spelled out rather than inferred - 2.12 cannot infer through the existential in Class[_]
      def runtimeClassOf(symbol: ru.ClassSymbol): Seq[Class[_]] =
        Try(mirror.runtimeClass(symbol)).toOption.toSeq
      val classes: Seq[Class[_]] = concreteBelow(rootSymbol).flatMap(runtimeClassOf).distinct
      val grouped: Map[String, Seq[Class[_]]] = classes.groupBy(clazz => SealedPolymorphism.typeNameFor(clazz))
      val unique: Map[String, Subtype] = grouped.collect {
        case (name, Seq(only)) => name -> Subtype(only, SealedPolymorphism.moduleInstance(only))
      }
      Table(sealedRoot = true, byName = unique, duplicates = grouped.filter(_._2.size > 1))
    }
  }

  // bounded for the same reason as the other caches in this package
  @volatile private var _cache: LookupCache[Class[_], Table] =
    DefaultLookupCacheFactory.createLookupCache(16, 1000)

  private[scala] def clearCache(): Unit = _cache.clear()
}
