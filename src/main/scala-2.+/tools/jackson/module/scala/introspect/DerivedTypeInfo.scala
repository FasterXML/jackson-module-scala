package tools.jackson.module.scala.introspect

import tools.jackson.module.scala.LookupCacheFactory

/**
 * Deriving what the JVM erases is a Scala 3 way of capturing it, so there is nothing to read here.
 * Scala 2 users register the same thing with `registerReferencedValueType`.
 *
 * Nothing is read, so nothing is cached and the factory goes unused. It is taken all the same, so
 * that the introspector builds this the same way whichever compiler it was built with.
 */
private[introspect] class DerivedTypeInfo(lookupCacheFactory: LookupCacheFactory) {
  def erasedTypeArguments(clazz: Class[_]): Seq[(String, Class[_])] = Seq.empty
}
