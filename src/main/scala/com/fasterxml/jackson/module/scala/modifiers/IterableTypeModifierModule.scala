package com.fasterxml.jackson.module.scala.modifiers

import com.fasterxml.jackson.module.scala.JacksonModule

private object IterableTypeModifier extends CollectionLikeTypeModifier {
  val BASE = classOf[Iterable[Any]]
}

trait IterableTypeModifierModule extends JacksonModule {
  this += IterableTypeModifier
}