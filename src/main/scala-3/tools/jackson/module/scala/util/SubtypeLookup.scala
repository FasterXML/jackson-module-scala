package tools.jackson.module.scala.util

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
 */
private[scala] class SubtypeLookup(polymorphism: SealedPolymorphism) {

  import SealedPolymorphism._


  private val EnumClass = classOf[scala.reflect.Enum]

  /** Scala 3 enums are tagged by the enum support, from an exact case table. */
  def isScalaEnum(clazz: Class[_]): Boolean = EnumClass.isAssignableFrom(clazz)

  /** Nothing extra is needed to look up a Scala 3 hierarchy. */
  def checkAvailable(clazz: Class[_]): Unit = ()

  /**
   * Scala 3 cannot enumerate a hierarchy, so a subclass can only be ruled out where the JVM proves
   * it: a final class, which covers the module class of every `case object`. A `case class` is not
   * final in bytecode and can be extended by a plain class, so it cannot be ruled out here - Scala 2
   * answers the same question exactly, from the hierarchy itself.
   */
  def mayHaveSubtypes(clazz: Class[_], root: Class[_]): Boolean = !Modifier.isFinal(clazz.getModifiers)

  def findSubtype(baseClass: Class[_], root: Class[_], typeName: String): Option[Subtype] = {
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

  /** Nothing is cached here - resolution is memoised on the SealedPolymorphism instance. */
  def clearCache(): Unit = ()
}
