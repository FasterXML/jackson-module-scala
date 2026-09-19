package tools.jackson.module.scala.introspect

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{Map => MutableMap}

/**
 * A type as a class captured it by deriving `ScalaTypeInfo`: the raw class and one shape per type
 * argument, a primitive being its primitive class. Mirrors `ScalaTypeInfo.TypeShape`, which only
 * exists on Scala 3, so that what the introspector holds is the same whichever compiler built it.
 */
private[introspect] final case class DerivedTypeShape(rawClass: Class[_], typeArguments: Seq[DerivedTypeShape])

/** A content type registered by hand with `registerReferencedValueType`. */
private[introspect] case class ClassHolder(valueClass: Option[Class[_]] = None)

// Concurrent so that a registration made while a mapper is already in use on another thread is
// neither lost nor read mid-resize. Declared as the general type so that what this holds stays an
// implementation detail.
private[introspect] case class ClassOverrides(overrides: MutableMap[String, ClassHolder] = TrieMap.empty)
