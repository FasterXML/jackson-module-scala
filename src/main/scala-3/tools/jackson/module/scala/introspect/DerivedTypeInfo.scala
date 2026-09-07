package tools.jackson.module.scala.introspect

import tools.jackson.databind.util.LookupCache
import tools.jackson.module.scala.{LookupCacheFactory, ScalaTypeInfo}

import scala.util.Try

/**
 * Reads what a class captured by deriving [[ScalaTypeInfo]].
 *
 * A `derives` leaves nothing on the class itself - no parent, no annotation - so the companion is
 * what marks it: the compiler puts a `derived$ScalaTypeInfo` there, and that is the only trace of it
 * at runtime.
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
  private val cache: LookupCache[Class[_], Seq[(String, Class[_])]] =
    lookupCacheFactory.createLookupCache(16, CacheSize)

  /**
   * What `clazz` captured by deriving [[ScalaTypeInfo]], or an empty table where it derived nothing.
   *
   * Asked once per bean descriptor built, so asked again for every class whose descriptor has since
   * been evicted - the descriptor cache holds 100 by default - and answered the same way every time.
   * The answer is therefore remembered, because reading it is not free: the great majority of classes
   * have no companion at all, and finding that out costs a `Class.forName` that ends in a
   * `ClassNotFoundException`, stack trace and all.
   */
  def erasedTypeArguments(clazz: Class[_]): Seq[(String, Class[_])] = {
    Option(cache.get(clazz)) match {
      case Some(arguments) => arguments
      case _ =>
        val arguments = readErasedTypeArguments(clazz)
        Option(cache.putIfAbsent(clazz, arguments)).getOrElse(arguments)
    }
  }

  private def readErasedTypeArguments(clazz: Class[_]): Seq[(String, Class[_])] = {
    Try {
      val loader = Option(clazz.getClassLoader).getOrElse(ClassLoader.getSystemClassLoader)
      val companion = Class.forName(clazz.getName + "$", false, loader)
      val instance = companion.getField("MODULE$").get(None.orNull)
      val derived = companion.getMethod(MethodName).invoke(instance).asInstanceOf[ScalaTypeInfo[_]]
      derived.erasedTypeArguments.map { case (field, argument) => (field, argument: Class[_]) }
    }.getOrElse(Seq.empty)
  }
}

private[introspect] object DerivedTypeInfo {
  private val MethodName = "derived$" + classOf[ScalaTypeInfo[_]].getSimpleName
  private val CacheSize = 1000
}
