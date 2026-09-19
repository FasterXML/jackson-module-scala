package tools.jackson.module.scala.introspect

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.util.ClassW
import tools.jackson.module.scala.{LookupCacheFactory, ScalaTypeInfo}

import scala.util.Try

/**
 * Reads what a class captured by deriving [[ScalaTypeInfo]].
 *
 * A `derives` leaves nothing on the class itself - no parent, no annotation - so the companion is
 * what marks it: the compiler puts a `derived$ScalaTypeInfo` there, and that is the only trace of it
 * at runtime. A `derives` on an enum or on a sealed base describes every case or implementation
 * below it and leaves the trace on the base's companion, so a class with nothing on its own
 * companion is looked for on the companions of its ancestors.
 *
 * One of these belongs to each [[ScalaAnnotationIntrospectorModule]] instance, so a module built
 * through [[tools.jackson.module.scala.ScalaModule.Builder]] remembers what it has read
 * independently of every other module and of the `ScalaAnnotationIntrospectorModule` object.
 */
private[introspect] class DerivedTypeInfo(lookupCacheFactory: LookupCacheFactory) {

  import DerivedTypeInfo._

  // Bounded for the same reason as the other caches that key on Class: a class loaded by a child
  // classloader (hot redeploy, OSGi, script engines) is not retained indefinitely. Keyed by Class
  // rather than by class name because the table holds Class instances - two same-named classes from
  // different classloaders must not share an entry. A pure memo, so an evicted entry is read again.
  private val cache: LookupCache[Class[_], Derived] =
    lookupCacheFactory.createLookupCache(16, CacheSize)

  /**
   * What was captured by deriving [[ScalaTypeInfo]] for the fields of `clazz`: by a mix-in registered
   * for it, first, then by the class itself, directly or through an enum or sealed base that derived
   * it - or an empty table where nothing did.
   *
   * A mix-in describes `clazz` either as a trait extending it, whose derives keys the members by
   * `clazz`, or as an unrelated class with members of the same names, keyed by the mix-in itself.
   *
   * Asked for every member of every Scala class Jackson types, and answered the same way every time.
   * The answer is therefore remembered, because reading it is not free: the great majority of classes
   * have no companion at all, and finding that out costs a `Class.forName` that ends in a
   * `ClassNotFoundException`, stack trace and all - once for the class and once for each ancestor.
   */
  def erasedFields(clazz: Class[_], mixin: Option[Class[_]]): Seq[(String, DerivedTypeShape)] = mixin match {
    case Some(m) => derived(m).fieldsFor(clazz) ++ derived(clazz).fields
    case _ => derived(clazz).fields
  }

  /** The same, for the parameters of the `@JsonCreator` methods on the companion of `clazz`. */
  def erasedCreatorParameters(clazz: Class[_], mixin: Option[Class[_]]): Seq[(DerivedCreatorParameter, DerivedTypeShape)] = mixin match {
    case Some(m) => derived(m).creatorParametersFor(clazz) ++ derived(clazz).creatorParameters
    case _ => derived(clazz).creatorParameters
  }

  private def derived(clazz: Class[_]): Derived = {
    Option(cache.get(clazz)) match {
      case Some(found) => found
      case _ =>
        val read = readDerived(clazz)
        Option(cache.putIfAbsent(clazz, read)).getOrElse(read)
    }
  }

  // Everything the trace nearest to the class captured, whichever classes it is keyed by: the
  // class's own companion first, then the base a hierarchy was marked at. The first trace found is
  // the one that describes the class, and nothing above it is looked at.
  private def readDerived(clazz: Class[_]): Derived = {
    (clazz +: ancestors(clazz)).iterator.flatMap(derivedOn).nextOption() match {
      case Some(info) =>
        new Derived(clazz,
          info.erasedFields.map { case (declaring, field, shape) => (declaring, field, toShape(shape)) },
          info.erasedCreatorParameters.map { case (parameter, shape) =>
            (parameter.rawClass, DerivedCreatorParameter(parameter.method, parameter.arity, parameter.index), toShape(shape))
          })
      case _ => new Derived(clazz, Seq.empty, Seq.empty)
    }
  }

  // A generic class derives a method that takes an instance for each type parameter. What is
  // captured does not depend on them - a field that mentions one is not described at all - so the
  // method is called with nothing for each.
  private def derivedOn(clazz: Class[_]): Option[ScalaTypeInfo[_]] = {
    ClassW.companionOf(clazz).flatMap { instance =>
      instance.getClass.getMethods.find(_.getName == MethodName).flatMap { method =>
        val arguments = Array.fill[AnyRef](method.getParameterCount)(None.orNull)
        Try(method.invoke(instance, arguments*).asInstanceOf[ScalaTypeInfo[_]]).toOption
      }
    }
  }

  // Superclasses and interfaces, nearest first, that could carry the trace: the enum an enum case
  // extends, or the sealed base an implementation extends, possibly through a sealed trait between.
  // Nothing from the standard library or the JDK is sealed by a user, so those are not looked at.
  private def ancestors(clazz: Class[_]): Seq[Class[_]] = {
    def isCandidate(c: Class[_]): Boolean =
      c != null && !c.getName.startsWith("java.") && !c.getName.startsWith("scala.")
    def walk(pending: List[Class[_]], seen: Vector[Class[_]]): Vector[Class[_]] = pending match {
      case Nil => seen
      case head :: rest if seen.contains(head) => walk(rest, seen)
      case head :: rest =>
        val parents = (Option(head.getSuperclass).toList ++ head.getInterfaces.toList).filter(isCandidate)
        walk(rest ++ parents, seen :+ head)
    }
    walk((Option(clazz.getSuperclass).toList ++ clazz.getInterfaces.toList).filter(isCandidate), Vector.empty)
  }

  private def toShape(shape: ScalaTypeInfo.TypeShape): DerivedTypeShape =
    DerivedTypeShape(shape.rawClass, shape.typeArguments.map(toShape))
}

private[introspect] object DerivedTypeInfo {
  private val MethodName = "derived$" + classOf[ScalaTypeInfo[_]].getSimpleName
  private val CacheSize = 1000

  /**
   * What the trace nearest to `clazz` captured, each entry keyed by the class it describes: fields,
   * and the parameters of companion creators.
   */
  private final class Derived(clazz: Class[_],
                              captured: Seq[(Class[_], String, DerivedTypeShape)],
                              capturedCreatorParameters: Seq[(Class[_], DerivedCreatorParameter, DerivedTypeShape)]) {

    /** The entries describing `clazz` itself. */
    val fields: Seq[(String, DerivedTypeShape)] =
      captured.collect { case (c, name, shape) if c == clazz => (name, shape) }
    val creatorParameters: Seq[(DerivedCreatorParameter, DerivedTypeShape)] =
      capturedCreatorParameters.collect { case (c, parameter, shape) if c == clazz => (parameter, shape) }

    /**
     * The entries describing `other`, when `clazz` is a mix-in registered for it: a trait that
     * extends `other` keys the members by `other`, an unrelated class with members of the same
     * names keys them by itself.
     */
    def fieldsFor(other: Class[_]): Seq[(String, DerivedTypeShape)] =
      captured.collect { case (c, name, shape) if c == other || c == clazz => (name, shape) }
    def creatorParametersFor(other: Class[_]): Seq[(DerivedCreatorParameter, DerivedTypeShape)] =
      capturedCreatorParameters.collect { case (c, parameter, shape) if c == other || c == clazz => (parameter, shape) }
  }
}
