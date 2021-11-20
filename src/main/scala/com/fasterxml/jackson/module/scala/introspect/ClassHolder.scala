package com.fasterxml.jackson.module.scala.introspect

import scala.collection.mutable.{Map => MutableMap}

private[introspect] case class ClassHolder(valueClass: Option[Class[_]] = None)
private[introspect] case class ClassOverrides(overrides: MutableMap[String, ClassHolder] = MutableMap.empty)

