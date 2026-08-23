package tools.jackson.module.scala.util

import tools.jackson.module.scala.util.SealedPolymorphism.Subtype

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
private[scala] object SubtypeLookup {

  private val EnumClass = classOf[scala.reflect.Enum]

  /** Scala 3 enums are tagged by the enum support, from an exact case table. */
  def isScalaEnum(clazz: Class[_]): Boolean = EnumClass.isAssignableFrom(clazz)

  /** Nothing extra is needed to look up a Scala 3 hierarchy. */
  def checkAvailable(clazz: Class[_]): Unit = ()

  def findSubtype(baseClass: Class[_], typeName: String): Option[Subtype] = {
    val loader = SealedPolymorphism.loaderFor(baseClass)
    // anchored on the root, so a property declared as an intermediate type still resolves the names
    // that were written for the hierarchy as a whole
    candidateNames(SealedPolymorphism.rootOf(baseClass).getName, typeName).iterator
      .flatMap(name => Try(Class.forName(name, false, loader)).toOption)
      // a concrete root names itself, so the base is only excluded when it cannot hold a value
      .filter(candidate => baseClass.isAssignableFrom(candidate) && SealedPolymorphism.isConcrete(candidate))
      .map(candidate => Subtype(candidate, SealedPolymorphism.moduleInstance(candidate)))
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
  def unreachableReason(clazz: Class[_]): Option[String] = {
    val root = SealedPolymorphism.rootOf(clazz)
    val typeName = SealedPolymorphism.typeNameFor(clazz)
    SealedPolymorphism.resolve(root, typeName) match {
      case Some(subtype) if subtype.clazz == clazz => None
      case Some(subtype) => Some(SealedPolymorphism.nameTakenMessage(clazz, typeName, subtype.clazz))
      case None => Some(SealedPolymorphism.notFoundMessage(clazz, typeName, root))
    }
  }

  /**
   * Class names a `sealed` implementation could have been compiled to. A candidate that is not a
   * subtype of the base is discarded by the caller, so the companion of a case class and the static
   * forwarder class of a case object are both ignored.
   */
  private def candidateNames(rootName: String, typeName: String): Seq[String] =
    // the object form is tried first - a case object's instances have the `$` class
    SealedPolymorphism.prefixesFor(rootName).flatMap(prefix => Seq(prefix + typeName + "$", prefix + typeName))
}
