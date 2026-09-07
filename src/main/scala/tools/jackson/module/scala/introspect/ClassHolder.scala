package tools.jackson.module.scala.introspect

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{Map => MutableMap}

private[introspect] case class ClassHolder(valueClass: Option[Class[_]] = None)

// The map is concurrent because a registration is no longer only something an application makes at
// startup: introspecting a class registers what it derived, on whatever thread got there first.
// Declared as the general type so that what this holds stays an implementation detail.
private[introspect] case class ClassOverrides(overrides: MutableMap[String, ClassHolder] = TrieMap.empty)
