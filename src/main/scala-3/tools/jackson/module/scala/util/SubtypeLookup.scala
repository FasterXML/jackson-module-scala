package tools.jackson.module.scala.util

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{DefaultLookupCacheFactory, SealedSubtypes}
import tools.jackson.module.scala.util.SealedPolymorphism.Subtype

import java.lang.reflect.Modifier
import scala.util.Try

/**
 * Finds the implementations of a marked hierarchy on Scala 3.
 *
 * Scala 3 records nothing about `sealed` on the JVM: scalac emits no `PermittedSubclasses`
 * attribute, and unlike an `enum` the companion carries no `Mirror.Sum`. Implementations are
 * therefore never enumerated. They do not need to be - writing a value only needs its own class,
 * and reading one only needs to turn a name back into a class - so a name is resolved by rebuilding
 * the class names an implementation could have been compiled to, from where the root is declared,
 * and keeping only candidates that really are subtypes of the base.
 *
 * Unless the hierarchy derives [[tools.jackson.module.scala.SealedSubtypes]], which captures its
 * implementations while the compiler still knows them. That table is exact, so it is preferred
 * wherever it exists and this falls back to rebuilding names only when it does not.
 */
private[scala] class SubtypeLookup(polymorphism: SealedPolymorphism) {

  import SealedPolymorphism._


  private val EnumClass = classOf[scala.reflect.Enum]

  /** Scala 3 enums are tagged by the enum support, from an exact case table. */
  def isScalaEnum(clazz: Class[_]): Boolean = EnumClass.isAssignableFrom(clazz)

  /** Nothing extra is needed to look up a Scala 3 hierarchy. */
  def checkAvailable(clazz: Class[_]): Unit = ()

  /**
   * True when the hierarchy `clazz` belongs to derives [[SealedSubtypes]], which marks it as surely
   * as extending the marker trait does.
   */
  def isDerived(clazz: Class[_]): Boolean = derivedRootOf(clazz).isDefined

  // the whole supertype graph: an implementation may sit below an intermediate sealed trait, and it
  // is the root of the hierarchy that derived the table
  private def derivedRootOf(clazz: Class[_]): Option[Class[_]] =
    if (derivedTable(clazz).isDefined) Some(clazz)
    else supertypesOf(clazz).view.flatMap(derivedRootOf).headOption

  /**
   * Scala 3 cannot enumerate a hierarchy, so a subclass can only be ruled out where the JVM proves
   * it: a final class, which covers the module class of every `case object`. A `case class` is not
   * final in bytecode and can be extended by a plain class, so it cannot be ruled out here - Scala 2
   * answers the same question exactly, from the hierarchy itself.
   */
  def mayHaveSubtypes(clazz: Class[_], root: Class[_]): Boolean = derivedTable(root) match {
    case Some(table) => table.exists(entry => entry.clazz != clazz && clazz.isAssignableFrom(entry.clazz))
    case None => !Modifier.isFinal(clazz.getModifiers)
  }

  def findSubtype(baseClass: Class[_], root: Class[_], typeName: String): Option[Subtype] =
    derivedTable(root) match {
      case Some(table) =>
        table.find(entry => polymorphism.typeNameFor(entry.clazz, root) == typeName)
          .filter(entry => baseClass.isAssignableFrom(entry.clazz))
      case None => findByName(baseClass, root, typeName)
    }

  private def findByName(baseClass: Class[_], root: Class[_], typeName: String): Option[Subtype] = {
    val loader = loaderFor(baseClass)
    // anchored on the root, so a property declared as an intermediate type still resolves the names
    // that were written for the hierarchy as a whole
    candidateNames(root.getName, typeName).iterator
      .flatMap(name => Try(Class.forName(name, false, loader)).toOption)
      // a concrete root names itself, so the base is only excluded when it cannot hold a value
      .filter(candidate => baseClass.isAssignableFrom(candidate) && isConcrete(candidate))
      .map(candidate => Subtype(candidate, moduleInstance(candidate)))
      .nextOption()
  }

  /**
   * Checks that an implementation can be found again from the name it is written under.
   *
   * Serializing needs only the value's own class, so without this an implementation that resolution
   * cannot reach would be written happily and fail only when something tried to read it back,
   * possibly in another process. Two things get caught: an implementation declared outside the
   * root's package, which `sealed` would have prevented, and one whose derived name is already taken
   * by another implementation declared closer to the root.
   */
  def unreachableReason(clazz: Class[_], root: Class[_]): Option[String] = {
    val typeName = polymorphism.typeNameFor(clazz, root)
    // a derived table names the whole hierarchy at once, so a name claimed twice is caught from
    // either side rather than only from whichever implementation resolution did not reach
    derivedTable(root).foreach { table =>
      val clashing = table.filter(entry => polymorphism.typeNameFor(entry.clazz, root) == typeName)
      if (clashing.length > 1) {
        return clashing.find(_.clazz != clazz).map(entry => nameTakenMessage(clazz, typeName, entry.clazz))
      }
    }
    polymorphism.resolve(root, root, typeName) match {
      case Some(subtype) if subtype.clazz == clazz => None
      case Some(subtype) => Some(nameTakenMessage(clazz, typeName, subtype.clazz))
      case None => Some(notFoundMessage(clazz, typeName, root))
    }
  }

  /**
   * Class names a `sealed` implementation could have been compiled to. A candidate that is not a
   * subtype of the base is discarded by the caller, so the companion of a case class and the static
   * forwarder class of a case object are both ignored.
   */
  private def candidateNames(rootName: String, typeName: String): Seq[String] = {
    // a derived name separates nesting with a dot, the JVM with a `$`
    val jvm = jvmName(typeName)
    // the object form is tried first - a case object's instances have the `$` class
    prefixesFor(rootName).flatMap(prefix => Seq(prefix + jvm + "$", prefix + jvm))
  }

  /**
   * The table a hierarchy captured by deriving [[SealedSubtypes]], read off its companion. The
   * compiler puts a `derived$SealedSubtypes` there, which is the only trace of it at runtime.
   */
  private def derivedTable(root: Class[_]): Option[Seq[Subtype]] = {
    val cache = _derived
    Option(cache.get(root)) match {
      case Some(table) => table
      case _ =>
        val table = readDerivedTable(root)
        Option(cache.putIfAbsent(root, table)).getOrElse(table)
    }
  }

  private def readDerivedTable(root: Class[_]): Option[Seq[Subtype]] = {
    Try {
      val companion = Class.forName(root.getName + "$", false, loaderFor(root))
      val module = companion.getField("MODULE$").get(None.orNull)
      val derived = companion.getMethod(DerivedMethodName).invoke(module).asInstanceOf[SealedSubtypes[_]]
      derived.subtypes.map { case (clazz, singleton) => Subtype(clazz, singleton) }
    }.toOption
  }

  private val DerivedMethodName = "derived$" + classOf[SealedSubtypes[_]].getSimpleName

  // bounded for the same reason as the other caches in this package
  @volatile private var _derived: LookupCache[Class[_], Option[Seq[Subtype]]] =
    DefaultLookupCacheFactory.createLookupCache(16, 1000)

  def clearCache(): Unit = _derived.clear()
}
