package tools.jackson.module.scala.introspect

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{Map => MutableMap}

/**
 * A type as a class captured it by deriving `ScalaTypeInfo`: the raw class and one shape per type
 * argument, a primitive being its primitive class. Mirrors `ScalaTypeInfo.TypeShape`, which only
 * exists on Scala 3, so that what the introspector holds is the same whichever compiler built it.
 */
private[introspect] final case class DerivedTypeShape(rawClass: Class[_], typeArguments: Seq[DerivedTypeShape])

/**
 * What is known about one field beyond its JVM signature. `valueClass` is a content type registered
 * by hand with `registerReferencedValueType`; `derivedType` is the whole type as captured by deriving
 * `ScalaTypeInfo`. Where both are set the hand registration is the one applied.
 */
private[introspect] case class ClassHolder(valueClass: Option[Class[_]] = None,
                                           derivedType: Option[DerivedTypeShape] = None)

// The map is concurrent because a registration is no longer only something an application makes at
// startup: introspecting a class registers what it derived, on whatever thread got there first.
// Declared as the general type so that what this holds stays an implementation detail.
private[introspect] case class ClassOverrides(overrides: MutableMap[String, ClassHolder] = TrieMap.empty)
