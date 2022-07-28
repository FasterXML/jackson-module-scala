package tools.jackson.module.scala

import tools.jackson.module.scala.ser.IterableSerializerModule

/**
 * Adds support for serializing Scala Iterables.
 */
trait IterableModule extends IterableSerializerModule

object IterableModule extends IterableModule
